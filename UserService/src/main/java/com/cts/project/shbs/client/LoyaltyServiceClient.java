package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import lombok.extern.slf4j.Slf4j;

@FeignClient(
    name = "loyalty-service",
    fallbackFactory = LoyaltyServiceClient.LoyaltyServiceFallbackFactory.class
)
public interface LoyaltyServiceClient {

    @PostMapping("/api/loyalty/initializeLoyaltyAccount/{userId}")
    ResponseEntity<String> initializeLoyaltyAccount(@PathVariable("userId") Long userId);

    @Component
    @Slf4j
    class LoyaltyServiceFallbackFactory implements FallbackFactory<LoyaltyServiceClient> {

        @Override
        public LoyaltyServiceClient create(Throwable cause) {
            return new LoyaltyServiceClient() {
                @Override
                public ResponseEntity<String> initializeLoyaltyAccount(Long userId) {

                    if (cause instanceof feign.FeignException.ServiceUnavailable) {
                        log.error("Loyalty service is DOWN (503) for userId: {}. " +
                                  "Cause: {}", userId, cause.getMessage());
                    } else if (cause instanceof feign.FeignException.GatewayTimeout) {
                        log.error("Loyalty service TIMED OUT for userId: {}. " +
                                  "Cause: {}", userId, cause.getMessage());
                    } else {
                        log.error("Loyalty service FAILED for userId: {}. " +
                                  "Cause: {}", userId, cause.getMessage());
                    }

                    return ResponseEntity.ok("Loyalty initialization deferred - service unavailable");
                }
            };
        }
    }
}