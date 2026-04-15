package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import feign.RetryableException;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BookingServiceClientFallbackFactory implements FallbackFactory<BookingServiceClient> {

    @Override
    public BookingServiceClient create(Throwable cause) {

        return new BookingServiceClient() {

            @Override
            public ResponseEntity<String> updateReviewStatus(Long bookingId, String status) {
            	
            	if (cause instanceof RetryableException) {
            	    log.error("[BookingService] Service not discoverable in Eureka " +
            	              "for bookingId={} | Reason: {}", bookingId, cause.getMessage());
            	    return ResponseEntity
            	            .status(HttpStatus.SERVICE_UNAVAILABLE)
            	            .body("Booking service not reachable for bookingId: " + bookingId);
            	}
                if (cause instanceof FeignException.ServiceUnavailable) {
                    log.error("[BookingService] Service is DOWN while updating review status " +
                              "for bookingId={}, status={} | Reason: {}", bookingId, status, cause.getMessage());
                    return ResponseEntity
                            .status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("Booking service is currently unavailable," +
                                  " review status update failed for bookingId: " + bookingId);
                }

                if (cause instanceof FeignException.GatewayTimeout) {
                    log.error("[BookingService] Timeout while updating review status " +
                              "for bookingId={} | Reason: {}", bookingId, cause.getMessage());
                    return ResponseEntity
                            .status(HttpStatus.GATEWAY_TIMEOUT)
                            .body("Request timed out while updating review status" +
                                  " for bookingId: " + bookingId + ", please retry");
                }

                if (cause instanceof FeignException.NotFound) {
                    log.warn("[BookingService] Booking not found for bookingId={}", bookingId);
                    return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body("No booking found with id: " + bookingId);
                }

                if (cause instanceof FeignException.BadRequest) {
                    log.warn("[BookingService] Invalid status={} for bookingId={}", status, bookingId);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body("Invalid review status: " + status + " for bookingId: " + bookingId);
                }

                // Generic fallback
                log.error("[BookingService] Unexpected error while updating review status " +
                          "for bookingId={}, status={} | Reason: {}", bookingId, status, cause.getMessage());
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Unexpected error occurred while updating review" +
                              " status for bookingId: " + bookingId);
            }
        };
    }
}