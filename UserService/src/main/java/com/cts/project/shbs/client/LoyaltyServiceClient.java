package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "loyalty-service",
        fallbackFactory = LoyaltyServiceFallbackFactory.class
)
public interface LoyaltyServiceClient {

    @PostMapping("/api/loyalty/initializeLoyaltyAccount/{userId}")
    ResponseEntity<String> initializeLoyaltyAccount(@PathVariable("userId") Long userId);
}