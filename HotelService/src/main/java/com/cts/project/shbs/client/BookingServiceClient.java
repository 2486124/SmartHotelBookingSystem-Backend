package com.cts.project.shbs.client;

import com.cts.project.shbs.dto.BookedRoomsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(
        name = "booking-service",
        fallbackFactory = BookingServiceFallbackFactory.class
)
public interface BookingServiceClient {

    @GetMapping("/api/bookings/booked-rooms")
    BookedRoomsResponse getBookedRooms(
            @RequestParam("hotelId") Long hotelId,
            @RequestParam("checkIn") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    );

    @PutMapping("/api/bookings/hotel/cancel-future/{hotelId}")
    String cancelFutureBookings(
            @PathVariable("hotelId") Long hotelId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role
    );
}