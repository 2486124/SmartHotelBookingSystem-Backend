package com.cts.project.shbs.service.impl;

import com.cts.project.shbs.dto.RazorpayConfirmRequest;
import com.cts.project.shbs.dto.RazorpayOrderRequest;
import com.cts.project.shbs.exception.CancellationNotAllowedException;
import com.cts.project.shbs.exception.InvalidBookingStatusException;
import com.cts.project.shbs.exception.ResourceNotFoundException;
import com.cts.project.shbs.exception.RoomNotAvailableException;
import com.cts.project.shbs.model.Booking;
import com.cts.project.shbs.model.Booking.BookingStatus;
import com.cts.project.shbs.model.Payment;
import com.cts.project.shbs.model.Payment.PaymentMethod;
import com.cts.project.shbs.model.Payment.PaymentStatus;
import com.cts.project.shbs.repository.BookingRepository;
import com.cts.project.shbs.repository.PaymentRepository;
import com.cts.project.shbs.service.LoyaltyIntegrationService;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepo;

    @Mock
    private PaymentRepository paymentRepo;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private LoyaltyIntegrationService loyaltyService; // ← ADDED

    @InjectMocks
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "keyId", "rzp_test_key");
        ReflectionTestUtils.setField(bookingService, "keySecret", "test_secret");
    }

    // ─── createOrder ───────────────────────────────────────────────

    @Test
    void createOrder_RoomAlreadyBooked_ThrowsException() {
        RazorpayOrderRequest request = new RazorpayOrderRequest();
        request.setUserId(1L);
        request.setRoomId(2L);
        request.setHotelId(1L);
        request.setCheckInDate(LocalDate.now().plusDays(10));
        request.setCheckOutDate(LocalDate.now().plusDays(13));
        request.setAmount(1500.0);
        request.setPaymentMethod(PaymentMethod.UPI);

        when(bookingRepo.findBookedRoomIds(any(), any(), any()))
                .thenReturn(List.of(2L));

        assertThrows(RoomNotAvailableException.class,
                () -> bookingService.createOrder(request));
    }

    // ─── confirmBooking ────────────────────────────────────────────

    @Test
    void confirmBooking_InvalidSignature_ThrowsException() {
        RazorpayConfirmRequest request = new RazorpayConfirmRequest();
        request.setUserId(1L);
        request.setRoomId(2L);
        request.setHotelId(1L);
        request.setCheckInDate(LocalDate.now().plusDays(10));
        request.setCheckOutDate(LocalDate.now().plusDays(13));
        request.setAmount(1500.0);
        request.setPaymentMethod(PaymentMethod.UPI);
        request.setRazorpayOrderId("order_test123");
        request.setRazorpayPaymentId("pay_test123");
        request.setRazorpaySignature("invalid_signature");

        assertThrows(RuntimeException.class,
                () -> bookingService.confirmBooking(request));
    }

    // ─── cancelBooking ─────────────────────────────────────────────

    @Test
    void cancelBooking_Success() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .userId(1L)
                .roomId(2L)
                .hotelId(1L)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .status(BookingStatus.CONFIRMED)
                .paymentId(1L)
                .build();

        Payment payment = Payment.builder()
                .paymentId(1L)
                .userId(1L)
                .amount(1500.0)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(bookingRepo.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(payment));

        // Loyalty service is mocked — does nothing (simulates it being down or working)
        doNothing().when(loyaltyService).cancelPoints(anyLong(), anyDouble(), anyLong());
        doNothing().when(loyaltyService).revertRedemption(anyLong(), anyLong());

        bookingService.cancelBooking(1L);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(bookingRepo, times(1)).save(booking);
        verify(paymentRepo, times(1)).save(payment);
        // Verify loyalty service was called
        verify(loyaltyService, times(1)).cancelPoints(anyLong(), anyDouble(), anyLong());
        verify(loyaltyService, times(1)).revertRedemption(anyLong(), anyLong());
    }

    @Test
    void cancelBooking_AlreadyCancelled_ThrowsException() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .status(BookingStatus.CANCELLED)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .build();

        when(bookingRepo.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(InvalidBookingStatusException.class,
                () -> bookingService.cancelBooking(1L));
    }

    @Test
    void cancelBooking_Within24Hours_ThrowsException() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .status(BookingStatus.CONFIRMED)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(3))
                .build();

        when(bookingRepo.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(CancellationNotAllowedException.class,
                () -> bookingService.cancelBooking(1L));
    }

    @Test
    void cancelBooking_NotFound_ThrowsException() {
        when(bookingRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.cancelBooking(999L));
    }

    // ─── updateStatus ──────────────────────────────────────────────

    @Test
    void updateStatus_Success() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .userId(1L)
                .status(BookingStatus.CONFIRMED)
                .build();

        Payment payment = Payment.builder()
                .paymentId(1L)
                .amount(1500.0)
                .build();

        when(bookingRepo.findById(1L)).thenReturn(Optional.of(booking));

        bookingService.updateStatus(1L, BookingStatus.CHECKED_IN);

        assertEquals(BookingStatus.CHECKED_IN, booking.getStatus());
        verify(bookingRepo).save(booking);
        // loyaltyService.confirmCheckout is only called on CHECKED_OUT, not CHECKED_IN
        verify(loyaltyService, never()).confirmCheckout(anyLong(), anyLong(), anyDouble());
    }

    @Test
    void updateStatus_CheckedOut_CallsLoyaltyConfirmCheckout() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .userId(1L)
                .status(BookingStatus.CHECKED_IN)
                .build();

        Payment payment = Payment.builder()
                .paymentId(1L)
                .amount(1500.0)
                .build();

        when(bookingRepo.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepo.findByBookingId(1L)).thenReturn(Optional.of(payment));
        doNothing().when(loyaltyService).confirmCheckout(anyLong(), anyLong(), anyDouble());

        bookingService.updateStatus(1L, BookingStatus.CHECKED_OUT);

        assertEquals(BookingStatus.CHECKED_OUT, booking.getStatus());
        verify(loyaltyService, times(1)).confirmCheckout(1L, 1L, 1500.0);
    }

    @Test
    void updateStatus_CancelledBooking_ThrowsException() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepo.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(InvalidBookingStatusException.class,
                () -> bookingService.updateStatus(1L, BookingStatus.CHECKED_IN));
    }

    @Test
    void updateStatus_NotFound_ThrowsException() {
        when(bookingRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.updateStatus(999L, BookingStatus.CHECKED_IN));
    }

    // ─── getBookingById ────────────────────────────────────────────

    @Test
    void getBookingById_Success() {
        Booking booking = Booking.builder().bookingId(1L).build();
        when(bookingRepo.findById(1L)).thenReturn(Optional.of(booking));

        Booking result = bookingService.getBookingById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getBookingId());
    }

    @Test
    void getBookingById_NotFound_ThrowsException() {
        when(bookingRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.getBookingById(999L));
    }

    // ─── getAllBookings ─────────────────────────────────────────────

    @Test
    void getAllBookings_ReturnsList() {
        List<Booking> bookings = Arrays.asList(
                Booking.builder().bookingId(1L).build(),
                Booking.builder().bookingId(2L).build()
        );
        when(bookingRepo.findAll()).thenReturn(bookings);

        List<Booking> result = bookingService.getAllBookings();

        assertEquals(2, result.size());
        verify(bookingRepo).findAll();
    }

    @Test
    void getAllBookings_EmptyList() {
        when(bookingRepo.findAll()).thenReturn(Collections.emptyList());

        List<Booking> result = bookingService.getAllBookings();

        assertTrue(result.isEmpty());
    }

    // ─── getBookingsByUserId ───────────────────────────────────────

    @Test
    void getBookingsByUserId_ReturnsList() {
        List<Booking> bookings = List.of(
                Booking.builder().bookingId(1L).userId(1L).build()
        );
        when(bookingRepo.findByUserId(1L)).thenReturn(bookings);

        List<Booking> result = bookingService.getBookingsByUserId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }

    // ─── getBookingsByHotelId ──────────────────────────────────────

    @Test
    void getBookingsByHotelId_ReturnsList() {
        List<Booking> bookings = List.of(
                Booking.builder().bookingId(1L).hotelId(1L).build(),
                Booking.builder().bookingId(2L).hotelId(1L).build()
        );
        when(bookingRepo.findByHotelId(1L)).thenReturn(bookings);

        List<Booking> result = bookingService.getBookingsByHotelId(1L);

        assertEquals(2, result.size());
    }

    // ─── getBookedRoomIds ──────────────────────────────────────────

    @Test
    void getBookedRoomIds_ReturnsRoomIds() {
        List<Long> roomIds = List.of(1L, 2L, 3L);
        when(bookingRepo.findBookedRoomIds(any(), any(), any()))
                .thenReturn(roomIds);

        List<Long> result = bookingService.getBookedRoomIds(
                1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
    }

    @Test
    void getBookedRoomIds_NoBookings_ReturnsEmpty() {
        when(bookingRepo.findBookedRoomIds(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<Long> result = bookingService.getBookedRoomIds(
                1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

        assertTrue(result.isEmpty());
    }

    // ─── cancelFutureBookingsByHotel ───────────────────────────────

    @Test
    void cancelFutureBookingsByHotel_Success() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .hotelId(1L)
                .status(BookingStatus.CONFIRMED)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(8))
                .paymentId(1L)
                .build();

        Payment payment = Payment.builder()
                .paymentId(1L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(bookingRepo.findFutureConfirmedBookingsByHotel(any(), any()))
                .thenReturn(List.of(booking));
        when(paymentRepo.findById(1L)).thenReturn(Optional.of(payment));

        String result = bookingService.cancelFutureBookingsByHotel(1L);

        assertTrue(result.contains("1 future booking(s) cancelled"));
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void cancelFutureBookingsByHotel_NoBookings() {
        when(bookingRepo.findFutureConfirmedBookingsByHotel(any(), any()))
                .thenReturn(Collections.emptyList());

        String result = bookingService.cancelFutureBookingsByHotel(1L);

        assertEquals("No future bookings found for Hotel ID: 1", result);
    }

    // ─── updateStatusByUserAndHotel ────────────────────────────────

    @Test
    void updateStatusByUserAndHotel_Success() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .userId(1L)
                .hotelId(1L)
                .status(BookingStatus.CHECKED_OUT)
                .build();

        when(bookingRepo.findCheckedOutBookingByUserAndHotel(1L, 1L))
                .thenReturn(Optional.of(booking));

        bookingService.updateStatusByUserAndHotel(1L, 1L, BookingStatus.REVIEWED);

        assertEquals(BookingStatus.REVIEWED, booking.getStatus());
        verify(bookingRepo).save(booking);
    }

    @Test
    void updateStatusByUserAndHotel_NotFound_ThrowsException() {
        when(bookingRepo.findCheckedOutBookingByUserAndHotel(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.updateStatusByUserAndHotel(1L, 1L, BookingStatus.REVIEWED));
    }
}