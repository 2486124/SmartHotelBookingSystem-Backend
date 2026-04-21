package com.cts.project.shbs.service;

import com.cts.project.shbs.client.LoyaltyServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoyaltyInitService {

    private final LoyaltyServiceClient loyaltyServiceClient;
    private static final String LOYALTY_CB = "loyaltyService";

    @CircuitBreaker(name = LOYALTY_CB, fallbackMethod = "loyaltyFallback")
    @Retry(name = LOYALTY_CB, fallbackMethod = "loyaltyFallback")
    public void initializeLoyaltyAccount(long userId) {
        log.info("Initializing loyalty account for GUEST user ID: {}", userId);
        loyaltyServiceClient.initializeLoyaltyAccount(userId);
        log.info("Loyalty account initialized successfully for user ID: {}", userId);
    }

    public void loyaltyFallback(long userId, Throwable ex) {
        log.warn("Loyalty service unavailable for user ID: {}. Reason: {}. " +
                "Account will be initialized on next retry job.", userId, ex.getMessage());
    }
}
