package com.cts.project.shbs.client;

import java.time.LocalDate;
import java.util.Collections;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.cts.project.shbs.dto.BookedRoomsResponse;
import com.cts.project.shbs.exception.BookingServiceException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BookingServiceFallbackFactory implements FallbackFactory<BookingServiceClient> {

    @Override
    public BookingServiceClient create(Throwable cause) {
        return new BookingServiceClient() {

            @Override
            public BookedRoomsResponse getBookedRooms(Long hotelId,
                    java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
                if (cause instanceof feign.FeignException.ServiceUnavailable) {
                    log.error("Booking service DOWN (503) — getBookedRooms hotelId: {}", hotelId);
                } else if (cause instanceof feign.FeignException.GatewayTimeout) {
                    log.error("Booking service TIMED OUT — getBookedRooms hotelId: {}", hotelId);
                } else {
                    log.error("Booking service FAILED — getBookedRooms hotelId: {}. Cause: {}",
                            hotelId, cause.getMessage());
                }
                throw new BookingServiceException(
                        "Booking service unavailable for hotelId: " + hotelId, cause);
            }

            @Override
            public String cancelFutureBookings(Long hotelId, String userId, String role) {
                log.warn("Booking service unavailable — could not cancel future bookings for hotelId: {}. " +
                        "Bookings may need manual cleanup. Cause: {}", hotelId, cause.getMessage());
                // Return gracefully so hotel deletion still proceeds
                return "Booking service unavailable — future bookings not cancelled for hotelId: " + hotelId;
            }
        };
    }
}