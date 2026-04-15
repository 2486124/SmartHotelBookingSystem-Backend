package com.cts.project.shbs.client;

import com.cts.project.shbs.dto.AddPointsRequest;
import com.cts.project.shbs.dto.CancelPointsRequestDto;
import com.cts.project.shbs.dto.RedemptionRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoyaltyServiceFallback implements LoyaltyServiceClient {

    @Override
    public Integer getPointsBalance(Long userId) {
        log.warn("Loyalty Service unavailable — getPointsBalance fallback for User ID: {}", userId);
        return 0;
    }

    @Override
    public Double previewDiscount(Integer points) {
        log.warn("Loyalty Service unavailable — previewDiscount fallback for points: {}", points);
        return 0.0;
    }

    @Override
    public Integer previewPoints(Double amount) {
        log.warn("Loyalty Service unavailable — previewPoints fallback for amount: {}", amount);
        return 0;
    }

    @Override
    public ResponseEntity<String> redeemPoints(RedemptionRequestDto request) {
        log.warn("Loyalty Service unavailable — redeemPoints fallback for User ID: {}", request.getUserId());
        return ResponseEntity.ok("Loyalty service unavailable — redemption skipped");
    }

    @Override
    public ResponseEntity<String> revertRedemption(Long userId, Long bookingId) {
        log.warn("Loyalty Service unavailable — revertRedemption fallback for Booking ID: {}", bookingId);
        return ResponseEntity.ok("Loyalty service unavailable — revert skipped");
    }

    @Override
    public ResponseEntity<String> addPendingPoints(AddPointsRequest request) {
        log.warn("Loyalty Service unavailable — addPendingPoints fallback for User ID: {}", request.getUserId());
        return ResponseEntity.ok("Loyalty service unavailable — points not added");
    }

    @Override
    public ResponseEntity<String> confirmCheckout(AddPointsRequest request) {
        log.warn("Loyalty Service unavailable — confirmCheckout fallback for User ID: {}", request.getUserId());
        return ResponseEntity.ok("Loyalty service unavailable — checkout points not confirmed");
    }

    @Override
    public ResponseEntity<String> cancelPoints(CancelPointsRequestDto request) {
        log.warn("Loyalty Service unavailable — cancelPoints fallback for User ID: {}", request.getUserId());
        return ResponseEntity.ok("Loyalty service unavailable — points not cancelled");
    }
}