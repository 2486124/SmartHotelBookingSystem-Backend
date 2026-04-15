package com.cts.project.shbs.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cts.project.shbs.client.HotelServiceClient;
import com.cts.project.shbs.dto.BookedRoomsResponse;
import com.cts.project.shbs.dto.HotelResponse;
import com.cts.project.shbs.dto.RazorpayConfirmRequest;
import com.cts.project.shbs.dto.RazorpayOrderRequest;
import com.cts.project.shbs.model.Booking;
import com.cts.project.shbs.model.Booking.BookingStatus;
import com.cts.project.shbs.service.BookingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Booking Service", description = "APIs for managing hotel bookings and payments")
@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final HotelServiceClient hotelServiceClient; // ← injected for ownership checks

    // ─────────────────────────────────────────────────────────────────
    // HELPER — fetches the manager's hotel and returns its hotelId
    // Throws FeignException (→ 404/500) if manager has no hotel
    // ─────────────────────────────────────────────────────────────────
    private Long getManagerHotelId(Long managerId) {
        HotelResponse hotel = hotelServiceClient.getHotelByManagerId(
                managerId, "ROLE_HOTEL_MANAGER");
        return hotel.getHotelId();
    }

    // ─────────────────────────────────────────────────────────────────
    // GUEST
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Step 1 — Create Razorpay order for booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Razorpay order created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed — invalid request body"),
        @ApiResponse(responseCode = "403", description = "Access denied — only GUEST can create bookings"),
        @ApiResponse(responseCode = "409", description = "Room is already booked for selected dates")
    })
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody RazorpayOrderRequest request) {
        log.info("Request received — Create order for User ID: {} Role: {}", userIdHeader, userRole);
        if (!userRole.equals("ROLE_GUEST")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only ROLE_GUEST can create bookings");
        }
        return ResponseEntity.ok(bookingService.createOrder(request));
    }

    @Operation(summary = "Step 2 — Verify payment and confirm booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment verified and booking confirmed"),
        @ApiResponse(responseCode = "400", description = "Payment verification failed — invalid signature"),
        @ApiResponse(responseCode = "403", description = "Access denied — only GUEST can confirm bookings")
    })
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmBooking(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody RazorpayConfirmRequest request) {
        log.info("Request received — Confirm booking for User ID: {} Role: {}", userIdHeader, userRole);
        if (!userRole.equals("ROLE_GUEST")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only GUEST can confirm bookings");
        }
        return ResponseEntity.ok(bookingService.confirmBooking(request));
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Get all bookings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All bookings returned successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied — only ADMIN can view all bookings")
    })
    @GetMapping
    public ResponseEntity<?> getAllBookings(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("Request received — Get all bookings by User ID: {}", userIdHeader);
        if (!userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only ADMIN can view all bookings");
        }
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    // ─────────────────────────────────────────────────────────────────
    // GUEST, HOTEL_MANAGER, ADMIN
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Get booking by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking found and returned"),
        @ApiResponse(responseCode = "403", description = "Access denied — insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Booking not found with given ID")
    })
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getBooking(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userIdHeader,  // ← added
            @RequestHeader("X-User-Role") String userRole) {
        log.info("Request received — Get booking ID: {}", id);

        if (!userRole.equals("ROLE_GUEST") && !userRole.equals("ROLE_HOTEL_MANAGER") && !userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only GUEST, HOTEL_MANAGER or ADMIN can view bookings");
        }

        Booking booking = bookingService.getBookingById(id);
        Long callerId = Long.parseLong(userIdHeader);

        // GUEST — can only view their own bookings
        if (userRole.equals("ROLE_GUEST") && !booking.getUserId().equals(callerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — you can only view your own bookings");
        }

        // HOTEL_MANAGER — can only view bookings for their own hotel
        if (userRole.equals("ROLE_HOTEL_MANAGER")) {
            Long managerHotelId = getManagerHotelId(callerId);
            if (!booking.getHotelId().equals(managerHotelId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access denied — this booking does not belong to your hotel");
            }
        }

        // ADMIN — no restriction, falls through
        return ResponseEntity.ok(booking);
    }

    // ─────────────────────────────────────────────────────────────────
    // GUEST — own bookings only
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Get all bookings by user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookings for user returned successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied — you can only view your own bookings")
    })
    @GetMapping("/user")
    public ResponseEntity<?> getUserBookings(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
    	Long userId = Long.parseLong(userIdHeader);
    	log.info("Request received — Get bookings for User ID: {}", userId);
        if (!userRole.equals("ROLE_GUEST") || !userIdHeader.equals(String.valueOf(userId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — you can only view your own bookings");
        }
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    @Operation(summary = "Get checked out booking status by user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Checked out bookings returned or no bookings message"),
        @ApiResponse(responseCode = "403", description = "Access denied — GUEST can only view their own bookings")
    })
    @GetMapping("/status/{userId}")
    public ResponseEntity<?> getBookingStatusByUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        log.info("Request received — Get checked out bookings for User ID: {}", userId);
        if (!userRole.equals("ROLE_GUEST") || !userIdHeader.equals(String.valueOf(userId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — GUEST can only view their own bookings");
        }
        List<Booking> bookings = bookingService.getBookingsByUserId(userId)
                .stream()
                .filter(b -> BookingStatus.CHECKED_OUT.equals(b.getStatus()))
                .toList();
        if (bookings.isEmpty()) {
            return ResponseEntity.ok("No checked out bookings found for User ID: " + userId);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Booking b : bookings) {
            Map<String, Object> map = new HashMap<>();
            map.put("bookingId", b.getBookingId());
            map.put("status", b.getStatus());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────
    // HOTEL_MANAGER — own hotel only
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Get all bookings by hotel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bookings for hotel returned successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied — only HOTEL_MANAGER can view hotel bookings")
    })
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<?> getBookingsByHotel(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long hotelId) {
        log.info("Request received — Get bookings for Hotel ID: {} by User ID: {}", hotelId, userIdHeader);
        if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only HOTEL_MANAGER can view hotel bookings");
        }

        // Ownership check — does this manager own this hotel?
        Long callerId = Long.parseLong(userIdHeader);
        Long managerHotelId = getManagerHotelId(callerId);
        if (!managerHotelId.equals(hotelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — you do not manage this hotel");
        }

        List<Booking> bookings = bookingService.getBookingsByHotelId(hotelId);
        if (bookings.isEmpty()) {
            return ResponseEntity.ok("No bookings found for Hotel ID: " + hotelId);
        }
        return ResponseEntity.ok(bookings);
    }

    // ─────────────────────────────────────────────────────────────────
    // INTER-SERVICE — called from hotel service (no auth, internal only)
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Get booked room IDs by hotel and date range")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booked room IDs returned or no rooms message"),
        @ApiResponse(responseCode = "400", description = "Check-out date must be after check-in date")
    })
    @GetMapping("/booked-rooms")
    public ResponseEntity<BookedRoomsResponse> getBookedRooms(
            @RequestParam Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        log.info("Request received — Get booked rooms for Hotel ID: {}", hotelId);
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            return ResponseEntity.badRequest().build();
        }
        List<Long> bookedRoomIds = bookingService.getBookedRoomIds(hotelId, checkIn, checkOut);
        BookedRoomsResponse response = new BookedRoomsResponse();
        response.setHotelId(hotelId);
        response.setCheckIn(checkIn);
        response.setCheckOut(checkOut);
        response.setBookedRoomIds(bookedRoomIds);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // HOTEL_MANAGER, ADMIN
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Cancel future bookings of a hotel before deletion")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Future bookings cancelled and payments refunded"),
        @ApiResponse(responseCode = "403", description = "Access denied — only HOTEL_MANAGER or ADMIN can cancel future bookings")
    })
    @PatchMapping("/hotel/cancel-future/{hotelId}")
    public ResponseEntity<?> cancelFutureBookings(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long hotelId) {
        log.info("Request received — Cancel future bookings for Hotel ID: {} by User ID: {}", hotelId, userIdHeader);
        if (!userRole.equals("ROLE_HOTEL_MANAGER") && !userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only HOTEL_MANAGER or ADMIN can cancel future bookings");
        }

        // HOTEL_MANAGER — must own the hotel before mass-cancelling its bookings
        if (userRole.equals("ROLE_HOTEL_MANAGER")) {
            Long callerId = Long.parseLong(userIdHeader);
            Long managerHotelId = getManagerHotelId(callerId);
            if (!managerHotelId.equals(hotelId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access denied — you do not manage this hotel");
            }
        }

        // ADMIN — no restriction, falls through
        return ResponseEntity.ok(bookingService.cancelFutureBookingsByHotel(hotelId));
    }

    // ─────────────────────────────────────────────────────────────────
    // GUEST, HOTEL_MANAGER
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Cancel a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "400", description = "Cancellation not allowed within 24 hours of check-in"),
        @ApiResponse(responseCode = "403", description = "Access denied — only GUEST or HOTEL_MANAGER can cancel bookings"),
        @ApiResponse(responseCode = "404", description = "Booking not found with given ID")
    })
    @PatchMapping("/cancel/{id}")
    public ResponseEntity<?> cancel(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long id) {
        log.info("Request received — Cancel booking ID: {} by User ID: {}", id, userIdHeader);
        if (!userRole.equals("ROLE_GUEST") && !userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only GUEST or HOTEL_MANAGER can cancel bookings");
        }

        Booking booking = bookingService.getBookingById(id);
        Long callerId = Long.parseLong(userIdHeader);

        // GUEST — can only cancel their own bookings
        if (userRole.equals("ROLE_GUEST") && !booking.getUserId().equals(callerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — you can only cancel your own bookings");
        }

        // HOTEL_MANAGER — can only cancel bookings for their own hotel
        if (userRole.equals("ROLE_HOTEL_MANAGER")) {
            Long managerHotelId = getManagerHotelId(callerId);
            if (!booking.getHotelId().equals(managerHotelId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Access denied — this booking does not belong to your hotel");
            }
        }

        bookingService.cancelBooking(id);
        return ResponseEntity.ok("Booking cancelled successfully.");
    }

    // ─────────────────────────────────────────────────────────────────
    // HOTEL_MANAGER — own hotel bookings only
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Update booking status to CHECKED_IN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking status updated to CHECKED_IN"),
        @ApiResponse(responseCode = "400", description = "Cannot update status of a cancelled booking"),
        @ApiResponse(responseCode = "403", description = "Access denied — only HOTEL_MANAGER can check in guests"),
        @ApiResponse(responseCode = "404", description = "Booking not found with given ID")
    })
    @PatchMapping("/checked-in/{id}")
    public ResponseEntity<?> checkIn(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long id) {
        log.info("Request received — Check in Booking ID: {} by User ID: {}", id, userIdHeader);
        if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only HOTEL_MANAGER can check in guests");
        }

        // Ownership check — booking must belong to this manager's hotel
        Booking booking = bookingService.getBookingById(id);
        Long callerId = Long.parseLong(userIdHeader);
        Long managerHotelId = getManagerHotelId(callerId);
        if (!booking.getHotelId().equals(managerHotelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — this booking does not belong to your hotel");
        }

        bookingService.updateStatus(id, BookingStatus.CHECKED_IN);
        return ResponseEntity.ok("Booking ID: " + id + " status updated to CHECKED_IN");
    }

    @Operation(summary = "Update booking status to CHECKED_OUT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking status updated to CHECKED_OUT"),
        @ApiResponse(responseCode = "400", description = "Cannot update status of a cancelled booking"),
        @ApiResponse(responseCode = "403", description = "Access denied — only HOTEL_MANAGER can check out guests"),
        @ApiResponse(responseCode = "404", description = "Booking not found with given ID")
    })
    @PatchMapping("/checked-out/{id}")
    public ResponseEntity<?> checkOut(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable Long id) {
        log.info("Request received — Check out Booking ID: {} by User ID: {}", id, userIdHeader);
        if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — only HOTEL_MANAGER can check out guests");
        }

        // Ownership check — booking must belong to this manager's hotel
        Booking booking = bookingService.getBookingById(id);
        Long callerId = Long.parseLong(userIdHeader);
        Long managerHotelId = getManagerHotelId(callerId);
        if (!booking.getHotelId().equals(managerHotelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — this booking does not belong to your hotel");
        }

        bookingService.updateStatus(id, BookingStatus.CHECKED_OUT);
        return ResponseEntity.ok("Booking ID: " + id + " status updated to CHECKED_OUT");
    }

    // ─────────────────────────────────────────────────────────────────
    // INTER-SERVICE — called from review service (no auth, internal only)
    // ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Update booking review status by bookingId")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking review status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status — use REVIEWED or NOT_REVIEWED"),
        @ApiResponse(responseCode = "404", description = "Booking not found with given ID")
    })
    @PutMapping("/{id}/review-status/{status}")
    public ResponseEntity<?> updateReviewStatus(
            @PathVariable Long id,
            @PathVariable String status) {
        log.info("Request received — Update review status for Booking ID: {} to {}", id, status);
        if (status.equalsIgnoreCase("REVIEWED")) {
            bookingService.updateStatus(id, BookingStatus.REVIEWED);
            return ResponseEntity.ok("Booking ID: " + id + " status updated to REVIEWED");
        } else if (status.equalsIgnoreCase("NOT_REVIEWED")) {
            bookingService.updateStatus(id, BookingStatus.NOT_REVIEWED);
            return ResponseEntity.ok("Booking ID: " + id + " status updated to NOT_REVIEWED");
        } else {
            return ResponseEntity.badRequest()
                    .body("Invalid status — use REVIEWED or NOT_REVIEWED");
        }
    }
}