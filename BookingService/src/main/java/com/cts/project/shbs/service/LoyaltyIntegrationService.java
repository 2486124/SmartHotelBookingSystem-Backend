package com.cts.project.shbs.service;

import com.cts.project.shbs.client.LoyaltyServiceClient;
import com.cts.project.shbs.dto.AddPointsRequest;
import com.cts.project.shbs.dto.CancelPointsRequestDto;
import com.cts.project.shbs.dto.RedemptionRequestDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyIntegrationService {

    private final LoyaltyServiceClient loyaltyServiceClient;

    @CircuitBreaker(name = "loyaltyService", fallbackMethod = "redeemPointsFallback")
    public void redeemPoints(Long userId, Long bookingId, Double amount) {
        RedemptionRequestDto request = RedemptionRequestDto.builder()
                .userId(userId)
                .bookingId(bookingId)
                .totalAmount(amount)
                .build();
        loyaltyServiceClient.redeemPoints(request);
        log.info("Loyalty points redeemed — User ID: {}", userId);
    }

    public void redeemPointsFallback(Long userId, Long bookingId, Double amount, Throwable t) {
        log.warn("Loyalty redeemPoints unavailable — fallback for User ID: {} — {}", userId, t.getMessage());
    }

    @CircuitBreaker(name = "loyaltyService", fallbackMethod = "addPendingPointsFallback")
    public void addPendingPoints(Long userId, Double amount, Long bookingId) {
        AddPointsRequest request = AddPointsRequest.builder()
                .userId(userId)
                .amountSpent(amount)
                .bookingId(bookingId)
                .build();
        loyaltyServiceClient.addPendingPoints(request);
        log.info("Loyalty pending points queued — User ID: {}, Amount: {}", userId, amount);
    }

    public void addPendingPointsFallback(Long userId, Double amount, Long bookingId, Throwable t) {
        log.warn("Loyalty addPendingPoints unavailable — fallback for User ID: {} — {}", userId, t.getMessage());
    }

    @CircuitBreaker(name = "loyaltyService", fallbackMethod = "cancelPointsFallback")
    public void cancelPoints(Long userId, Double amount, Long bookingId) {
        CancelPointsRequestDto request = CancelPointsRequestDto.builder()
                .userId(userId)
                .amount(amount)
                .build();
        loyaltyServiceClient.cancelPoints(request);
        log.info("Loyalty pending points removed — User ID: {}, Amount: {}", userId, amount);
    }

    public void cancelPointsFallback(Long userId, Double amount, Long bookingId, Throwable t) {
        log.warn("Loyalty cancelPoints unavailable — fallback for User ID: {} — {}", userId, t.getMessage());
    }

    @CircuitBreaker(name = "loyaltyService", fallbackMethod = "revertRedemptionFallback")
    public void revertRedemption(Long userId, Long bookingId) {
        loyaltyServiceClient.revertRedemption(userId, bookingId);
        log.info("Loyalty redemption reverted — User ID: {}, Booking ID: {}", userId, bookingId);
    }

    public void revertRedemptionFallback(Long userId, Long bookingId, Throwable t) {
        log.warn("Loyalty revertRedemption unavailable — fallback for Booking ID: {} — {}", bookingId, t.getMessage());
    }

    @CircuitBreaker(name = "loyaltyService", fallbackMethod = "confirmCheckoutFallback")
    public void confirmCheckout(Long userId, Long bookingId, Double amount) {
        AddPointsRequest request = AddPointsRequest.builder()
                .userId(userId)
                .bookingId(bookingId)
                .amountSpent(amount)
                .build();
        loyaltyServiceClient.confirmCheckout(request);
        log.info("Loyalty checkout confirmed — User ID: {}, Booking ID: {}", userId, bookingId);
    }

    public void confirmCheckoutFallback(Long userId, Long bookingId, Double amount, Throwable t) {
        log.warn("Loyalty confirmCheckout unavailable — fallback for Booking ID: {} — {}", bookingId, t.getMessage());
    }
}