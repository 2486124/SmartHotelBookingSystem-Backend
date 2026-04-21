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
        return (hotelId, checkIn, checkOut) -> {
            if (cause instanceof feign.FeignException.ServiceUnavailable) {
                log.error("Booking service is DOWN (503) for hotelId: {}. Cause: {}",
                        hotelId, cause.getMessage());
            } else if (cause instanceof feign.FeignException.GatewayTimeout) {
                log.error("Booking service TIMED OUT for hotelId: {}. Cause: {}",
                        hotelId, cause.getMessage());
            } else {
                log.error("Booking service FAILED for hotelId: {}. Cause: {}",
                        hotelId, cause.getMessage());
            }
            throw new BookingServiceException(
                    "Booking service unavailable for hotelId: " + hotelId, cause
            );
        };
    }
}