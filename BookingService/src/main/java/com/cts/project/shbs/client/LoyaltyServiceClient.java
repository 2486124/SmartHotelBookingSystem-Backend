package com.cts.project.shbs.client;

import com.cts.project.shbs.dto.AddPointsRequest;
import com.cts.project.shbs.dto.CancelPointsRequestDto;
import com.cts.project.shbs.dto.RedemptionRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "loyalty-service", url = "${loyalty.service.url:http://localhost:8085}", fallback = LoyaltyServiceFallback.class)
public interface LoyaltyServiceClient {

	@GetMapping("/api/loyalty/balance/{userId}")
	Integer getPointsBalance(@PathVariable("userId") Long userId);

	@GetMapping("/api/loyalty/calculate-discount/{points}")
	Double previewDiscount(@PathVariable("points") Integer points);

	@GetMapping("/api/loyalty/calculate-points/{amount}")
	Integer previewPoints(@PathVariable("amount") Double amount);

	@PostMapping("/api/loyalty/redeem")
	ResponseEntity<String> redeemPoints(@RequestBody RedemptionRequestDto request);

	@PostMapping("/api/loyalty/revert-redemption/{userId}/{bookingId}")
	ResponseEntity<String> revertRedemption(@PathVariable("userId") Long userId,
			@PathVariable("bookingId") Long bookingId);

	@PostMapping("/api/loyalty/add-pending-points")
	ResponseEntity<String> addPendingPoints(@RequestBody AddPointsRequest request);

	@PostMapping("/api/loyalty/confirm-checkout")
	ResponseEntity<String> confirmCheckout(@RequestBody AddPointsRequest request);

	@PutMapping("/api/loyalty/cancel-points")
	ResponseEntity<String> cancelPoints(@RequestBody CancelPointsRequestDto request);
}