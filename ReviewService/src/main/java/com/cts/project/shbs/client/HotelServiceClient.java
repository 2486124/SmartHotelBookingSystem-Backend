package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.cts.project.shbs.dto.HotelResponse;

@FeignClient(name = "hotel-service", fallbackFactory=HotelServiceClientFallbackFactory.class)
public interface HotelServiceClient {

    @PutMapping("/api/hotels/{hotelId}/rating/{rating}")
    ResponseEntity<Void> updateHotelRating(
            @PathVariable Long hotelId,
            @PathVariable Double rating);
    
    @GetMapping("/api/hotels/manager")
    HotelResponse getHotelByManagerId(
        @RequestHeader("X-User-Id") Long managerId,
        @RequestHeader("X-User-Role") String role
    );
}