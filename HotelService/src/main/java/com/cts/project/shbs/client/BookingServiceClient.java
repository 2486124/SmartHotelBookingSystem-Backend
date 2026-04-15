package com.cts.project.shbs.client;

import java.time.LocalDate;
import java.util.Collections;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cts.project.shbs.dto.BookedRoomsResponse;

import lombok.extern.slf4j.Slf4j;

@FeignClient(
    name = "booking-service",
    fallbackFactory = BookingServiceClient.BookingServiceFallbackFactory.class
)
public interface BookingServiceClient {

    @GetMapping("/api/bookings/booked-rooms")
    BookedRoomsResponse getBookedRooms(
        @RequestParam("hotelId")  Long hotelId,
        @RequestParam("checkIn")  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @RequestParam("checkOut") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    );

    @Component
    @Slf4j
    class BookingServiceFallbackFactory implements FallbackFactory<BookingServiceClient> {

        @Override
        public BookingServiceClient create(Throwable cause) {
            return new BookingServiceClient() {
                @Override
                public BookedRoomsResponse getBookedRooms(Long hotelId,
                        LocalDate checkIn, LocalDate checkOut) {

                    if (cause instanceof feign.FeignException.ServiceUnavailable) {
                        log.error("Booking service is DOWN (503) for hotelId: {}. " +
                                  "Cause: {}", hotelId, cause.getMessage());
                    } else if (cause instanceof feign.FeignException.GatewayTimeout) {
                        log.error("Booking service TIMED OUT for hotelId: {}. " +
                                  "Cause: {}", hotelId, cause.getMessage());
                    } else {
                        log.error("Booking service FAILED for hotelId: {}. " +
                                  "Cause: {}", hotelId, cause.getMessage());
                    }

                    BookedRoomsResponse fallback = new BookedRoomsResponse();
                    fallback.setBookedRoomIds(Collections.emptyList());
                    return fallback;
                }
            };
        }
    }
}