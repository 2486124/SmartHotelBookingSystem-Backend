package com.cts.project.shbs.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.cts.project.shbs.exception.LoyaltyServiceException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoyaltyServiceFallbackFactory implements FallbackFactory<LoyaltyServiceClient> {

    @Override
    public LoyaltyServiceClient create(Throwable cause) {
        return userId -> {
            if (cause instanceof feign.FeignException.ServiceUnavailable) {
                log.error("Loyalty service is DOWN (503) for userId: {}. Cause: {}",
                        userId, cause.getMessage());
            } else if (cause instanceof feign.FeignException.GatewayTimeout) {
                log.error("Loyalty service TIMED OUT for userId: {}. Cause: {}",
                        userId, cause.getMessage());
            } else {
                log.error("Loyalty service FAILED for userId: {}. Cause: {}",
                        userId, cause.getMessage());
            }
            throw new LoyaltyServiceException("Loyalty service unavailable for userId: " + userId, cause);
        };
    }
}