package com.cts.project.shbs.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.project.shbs.dto.BookingPaymentResponse;
import com.cts.project.shbs.dto.RazorpayConfirmRequest;
import com.cts.project.shbs.dto.RazorpayOrderRequest;
import com.cts.project.shbs.dto.RazorpayOrderResponse;
import com.cts.project.shbs.exception.CancellationNotAllowedException;
import com.cts.project.shbs.exception.InvalidBookingStatusException;
import com.cts.project.shbs.exception.ResourceNotFoundException;
import com.cts.project.shbs.exception.RoomNotAvailableException;
import com.cts.project.shbs.model.Booking;
import com.cts.project.shbs.model.Booking.BookingStatus;
import com.cts.project.shbs.model.Payment;
import com.cts.project.shbs.model.Payment.PaymentStatus;
import com.cts.project.shbs.repository.BookingRepository;
import com.cts.project.shbs.repository.PaymentRepository;
import com.cts.project.shbs.service.BookingService;
import com.cts.project.shbs.service.LoyaltyIntegrationService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final RazorpayClient razorpayClient;
    private final LoyaltyIntegrationService loyaltyService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Override
    public RazorpayOrderResponse createOrder(RazorpayOrderRequest request) {
        log.info("Creating Razorpay order — User ID: {}", request.getUserId());

        boolean roomAlreadyBooked = bookingRepo.findBookedRoomIds(
                request.getHotelId(), request.getCheckInDate(), request.getCheckOutDate())
                .contains(request.getRoomId());

        if (roomAlreadyBooked) {
            log.warn("Room ID: {} is already booked", request.getRoomId());
            throw new RoomNotAvailableException("Room ID: " + request.getRoomId() + " is already booked");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", (long)(request.getAmount() * 100));
            options.put("currency", "INR");
            options.put("receipt", "receipt_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(options);
            String orderId = order.get("id").toString();
            log.info("Razorpay order created — Order ID: {}", orderId);

            return RazorpayOrderResponse.builder()
                    .razorpayOrderId(orderId)
                    .amount(request.getAmount())
                    .currency("INR")
                    .keyId(keyId)
                    .userId(request.getUserId())
                    .roomId(request.getRoomId())
                    .hotelId(request.getHotelId())
                    .checkInDate(request.getCheckInDate())
                    .checkOutDate(request.getCheckOutDate())
                    .paymentMethod(request.getPaymentMethod())
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BookingPaymentResponse confirmBooking(RazorpayConfirmRequest request) {
        log.info("Confirming booking — Order ID: {}", request.getRazorpayOrderId());

        if (!verifySignature(request.getRazorpayOrderId(),
                             request.getRazorpayPaymentId(),
                             request.getRazorpaySignature())) {
            log.error("Razorpay signature verification failed");
            throw new RuntimeException("Payment verification failed — invalid signature");
        }

        log.info("Signature verified — saving payment and booking");

        double finalAmount = request.isRedeemPoints()
                ? Math.round(request.getAmount() * 0.90 * 100.0) / 100.0
                : request.getAmount();
        log.info("Final amount — Original: {}, Redeem: {}, Final: {}",
                request.getAmount(), request.isRedeemPoints(), finalAmount);

        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .amount(finalAmount)
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.SUCCESS)
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .build();
        Payment savedPayment = paymentRepo.save(payment);
        log.info("Payment saved — Payment ID: {}", savedPayment.getPaymentId());

        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .roomId(request.getRoomId())
                .hotelId(request.getHotelId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .status(BookingStatus.CONFIRMED)
                .paymentId(savedPayment.getPaymentId())
                .build();
        Booking savedBooking = bookingRepo.save(booking);
        log.info("Booking confirmed — Booking ID: {}", savedBooking.getBookingId());

        paymentRepo.updateBookingId(savedPayment.getPaymentId(), savedBooking.getBookingId());
        log.info("Payment linked to Booking ID: {}", savedBooking.getBookingId());

        if (request.isRedeemPoints()) {
            loyaltyService.redeemPoints(request.getUserId(), savedBooking.getBookingId(), request.getAmount());
        }

        loyaltyService.addPendingPoints(request.getUserId(), finalAmount, savedBooking.getBookingId());

        return BookingPaymentResponse.builder()
                .bookingId(savedBooking.getBookingId())
                .userId(savedBooking.getUserId())
                .roomId(savedBooking.getRoomId())
                .hotelId(savedBooking.getHotelId())
                .bookingStatus(savedBooking.getStatus())
                .paymentId(savedPayment.getPaymentId())
                .paymentStatus(savedPayment.getStatus())
                .paymentMethod(savedPayment.getPaymentMethod())
                .amount(savedPayment.getAmount())
                .razorpayOrderId(savedPayment.getRazorpayOrderId())
                .razorpayPaymentId(savedPayment.getRazorpayPaymentId())
                .build();
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        log.info("Cancelling booking — Booking ID: {}", bookingId);

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("Booking not found — ID: {}", bookingId);
                    return new ResourceNotFoundException("Booking not found with ID: " + bookingId);
                });

        // Block cancellation for any terminal or in-progress status
        switch (booking.getStatus()) {
            case CANCELLED:
                log.warn("Booking ID: {} is already cancelled", bookingId);
                throw new InvalidBookingStatusException("Booking ID: " + bookingId + " is already cancelled");
            case CHECKED_IN:
                log.warn("Booking ID: {} is already checked in — cannot cancel", bookingId);
                throw new InvalidBookingStatusException("Cannot cancel a booking that is already checked in");
            case CHECKED_OUT:
                log.warn("Booking ID: {} is already checked out — cannot cancel", bookingId);
                throw new InvalidBookingStatusException("Cannot cancel a booking that is already checked out");
            case REVIEWED:
            case NOT_REVIEWED:
                log.warn("Booking ID: {} stay is completed — cannot cancel", bookingId);
                throw new InvalidBookingStatusException("Cannot cancel a booking for a completed stay");
            default:
                break; // CONFIRMED — proceed
        }

        // Block cancellation within 24 hours of check-in
        LocalDateTime checkInDateTime = booking.getCheckInDate().atStartOfDay();
        if (LocalDateTime.now().plusHours(24).isAfter(checkInDateTime)) {
            log.warn("Cancellation rejected — within 24 hours for Booking ID: {}", bookingId);
            throw new CancellationNotAllowedException("Cancellations are only allowed 24 hours before check-in");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        // Process refund if payment exists
        final double[] paidAmount = {0.0};
        if (booking.getPaymentId() != null) {
            paymentRepo.findById(booking.getPaymentId()).ifPresent(payment -> {
                paidAmount[0] = payment.getAmount();
                initiateRazorpayRefund(payment);
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepo.save(payment);
                log.info("Payment ID: {} marked as REFUNDED", payment.getPaymentId());
            });
        }

        log.info("Booking ID: {} cancelled successfully", bookingId);

        loyaltyService.cancelPoints(booking.getUserId(), paidAmount[0], bookingId);
        loyaltyService.revertRedemption(booking.getUserId(), bookingId);
    }

    @Override
    @Transactional
    public String cancelFutureBookingsByHotel(Long hotelId) {
        log.info("Cancelling future bookings for Hotel ID: {}", hotelId);

        List<Booking> futureBookings = bookingRepo
                .findFutureConfirmedBookingsByHotel(hotelId, LocalDate.now());

        if (futureBookings.isEmpty()) {
            log.warn("No future confirmed bookings found for Hotel ID: {}", hotelId);
            return "No future bookings found for Hotel ID: " + hotelId;
        }

        int cancelledCount = 0;
        int refundedCount = 0;

        for (Booking booking : futureBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepo.save(booking);
            cancelledCount++;
            log.info("Booking ID: {} cancelled", booking.getBookingId());

            if (booking.getPaymentId() != null) {
                Payment payment = paymentRepo.findById(booking.getPaymentId()).orElse(null);
                if (payment != null) {
                    initiateRazorpayRefund(payment);
                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepo.save(payment);
                    refundedCount++;
                    log.info("Payment ID: {} marked as REFUNDED", payment.getPaymentId());
                }
            }
        }

        log.info("{} booking(s) cancelled and {} payment(s) refunded for Hotel ID: {}",
                cancelledCount, refundedCount, hotelId);

        return String.format("%d future booking(s) cancelled and %d payment(s) marked as refunded for Hotel ID: %d",
                cancelledCount, refundedCount, hotelId);
    }

    private void initiateRazorpayRefund(Payment payment) {
        try {
            if (payment.getRazorpayPaymentId() != null) {
                JSONObject refundOptions = new JSONObject();
                refundOptions.put("amount", (long)(payment.getAmount() * 100));
                razorpayClient.payments.refund(payment.getRazorpayPaymentId(), refundOptions);
                log.info("Razorpay refund initiated for Payment: {}", payment.getRazorpayPaymentId());
            }
        } catch (RazorpayException e) {
            log.error("Razorpay refund failed: {}", e.getMessage());
        }
    }

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void updateStatus(Long bookingId, BookingStatus status) {
        log.info("Updating Booking ID: {} to status: {}", bookingId, status);
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("Booking not found — ID: {}", bookingId);
                    return new ResourceNotFoundException("Booking not found with ID: " + bookingId);
                });
        if (BookingStatus.CANCELLED.equals(booking.getStatus())) {
            log.warn("Cannot update — Booking ID: {} is cancelled", bookingId);
            throw new InvalidBookingStatusException("Cannot update status of a cancelled booking");
        }
        booking.setStatus(status);
        bookingRepo.save(booking);
        log.info("Booking ID: {} status updated to {}", bookingId, status);

        if (BookingStatus.CHECKED_OUT.equals(status)) {
            paymentRepo.findByBookingId(bookingId).ifPresent(payment ->
                loyaltyService.confirmCheckout(booking.getUserId(), bookingId, payment.getAmount())
            );
        }
    }

    @Override
    public void updateStatusByUserAndHotel(Long userId, Long hotelId, BookingStatus status) {
        log.info("Updating status for User ID: {} Hotel ID: {} to {}", userId, hotelId, status);
        Booking booking = bookingRepo.findCheckedOutBookingByUserAndHotel(userId, hotelId)
                .orElseThrow(() -> {
                    log.error("No checked out booking for User ID: {} Hotel ID: {}", userId, hotelId);
                    return new ResourceNotFoundException(
                            "No checked out booking found for User ID: " + userId + " and Hotel ID: " + hotelId);
                });
        booking.setStatus(status);
        bookingRepo.save(booking);
        log.info("Booking ID: {} status updated to {}", booking.getBookingId(), status);
    }

    @Override
    public Booking getBookingById(Long id) {
        log.info("Fetching booking — ID: {}", id);
        return bookingRepo.findById(id)
                .orElseThrow(() -> {
                    log.error("Booking not found — ID: {}", id);
                    return new ResourceNotFoundException("Booking not found with ID: " + id);
                });
    }

    @Override
    public List<Booking> getAllBookings() {
        log.info("Fetching all bookings");
        List<Booking> bookings = bookingRepo.findAll();
        log.info("Total bookings found: {}", bookings.size());
        return bookings;
    }

    @Override
    public List<Booking> getBookingsByUserId(Long userId) {
        log.info("Fetching bookings for User ID: {}", userId);
        List<Booking> bookings = bookingRepo.findByUserId(userId);
        log.info("Total bookings for User ID {}: {}", userId, bookings.size());
        return bookings;
    }

    @Override
    public List<Booking> getBookingsByHotelId(Long hotelId) {
        log.info("Fetching bookings for Hotel ID: {}", hotelId);
        List<Booking> bookings = bookingRepo.findByHotelId(hotelId);
        log.info("Total bookings for Hotel ID {}: {}", hotelId, bookings.size());
        return bookings;
    }

    @Override
    public List<Long> getBookedRoomIds(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        log.info("Fetching booked room IDs for Hotel ID: {}", hotelId);
        List<Long> roomIds = bookingRepo.findBookedRoomIds(hotelId, checkIn, checkOut);
        log.info("Booked room IDs: {}", roomIds);
        return roomIds;
    }
}