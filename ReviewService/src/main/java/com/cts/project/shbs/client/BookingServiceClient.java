package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "booking-service", fallbackFactory=BookingServiceClientFallbackFactory.class)
public interface BookingServiceClient {

    @PutMapping("/api/bookings/{id}/review-status/{status}")
    ResponseEntity<String> updateReviewStatus(
            @PathVariable("id") Long bookingId,
            @PathVariable("status") String status);
}