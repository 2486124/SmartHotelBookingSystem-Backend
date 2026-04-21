package com.cts.project.shbs.client;

import com.cts.project.shbs.dto.BookedRoomsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
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
}