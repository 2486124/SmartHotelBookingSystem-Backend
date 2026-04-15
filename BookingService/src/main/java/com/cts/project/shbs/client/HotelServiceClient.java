package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.cts.project.shbs.dto.HotelResponse;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/api/hotels/manager")
    HotelResponse getHotelByManagerId(
        @RequestHeader("X-User-Id") Long managerId,
        @RequestHeader("X-User-Role") String role
    );
}
