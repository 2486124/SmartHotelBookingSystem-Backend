package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "hotel-service",
        fallbackFactory = HotelServiceFallbackFactory.class
)
public interface HotelServiceClient {

    @DeleteMapping("/api/hotels/delete-hotel")
    void deleteManagerHotel(
            @RequestHeader("X-User-Id") String managerId,
            @RequestHeader("X-User-Role") String role
    );
}
