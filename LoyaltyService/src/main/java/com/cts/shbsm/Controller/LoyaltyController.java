package com.cts.shbsm.Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cts.shbsm.dto.AddPointsRequestDto;
import com.cts.shbsm.dto.CancelPointsRequestDto;
import com.cts.shbsm.dto.RedemptionRequestDto;
import com.cts.shbsm.dto.RedemptionResponseDto;
import com.cts.shbsm.dto.ErrorDetailsDto;
import com.cts.shbsm.service.LoyaltyService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@Tag(name = "Loyalty Management", description = "Operations related to point accumulation and redemption")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    
    @Value("${loyalty.min.redemption.points}")
    private int minRedemptionPoints;

    @Operation(summary = "Add Pending Points", description = "Adds loyalty points to the user's pending balance based on the amount spent on a booking. Called internally by BookingService after a confirmed booking.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Points added to pending balance successfully"),
        @ApiResponse(responseCode = "404", description = "Loyalty account not found for the given userId",
                content = @Content(schema = @Schema(implementation = ErrorDetailsDto.class)))
    })
    @PostMapping("/add-pending-points")
    public ResponseEntity<String> addPoints(
            @Valid @RequestBody AddPointsRequestDto request) {
        loyaltyService.addLoyaltyPendingPoints(request.getUserId(), request.getAmountSpent());
        return ResponseEntity.ok("Points added. Request processed for User Id: " + request.getUserId());
    }

    @Operation(summary = "Redeem Points", description = "Redeems 300 loyalty points for a 10% discount on the booking amount. Requires a minimum balance of 300 points.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Redemption successful — 300 points deducted and discount applied"),
        @ApiResponse(responseCode = "400", description = "Minimum 300 points required for redemption",
                content = @Content(schema = @Schema(implementation = ErrorDetailsDto.class))),
        @ApiResponse(responseCode = "404", description = "Loyalty account not found for the given userId",
                content = @Content(schema = @Schema(implementation = ErrorDetailsDto.class)))
    })
    @PostMapping("/redeem")
    public ResponseEntity<String> redeemPoints(@Valid @RequestBody RedemptionRequestDto request) {
        // Fetch current points balance for the user
        Integer userPoints = loyaltyService.getPointsBalance(request.getUserId());
        // Validate: minimum 300 points required
        if (userPoints == null || userPoints < 300) {
            return ResponseEntity.badRequest()
                    .body("Minimum 300 points required for redemption. Current balance: " + userPoints);
        }
        // Calculate 10% discount on total amount
        Double autoDiscount = loyaltyService.calculateDiscount(request.getTotalAmount());
        // Deduct exactly 300 points from user's balance
        loyaltyService.adjustLoyaltyBalance(request.getUserId());
        // Save redemption record with 300 points deducted
        loyaltyService.saveRedemptionRecord(request.getUserId(), request.getBookingId(), 300, autoDiscount);
        return ResponseEntity.ok("Redemption successful for User ID: " + request.getUserId()
                + " | Discount Applied: " + autoDiscount
                + " | Points Deducted: 300");
    }

    @Operation(summary = "Confirm Checkout", description = "Moves pending loyalty points to the user's available balance after a successful checkout. Called internally by BookingService when a booking status changes to CHECKED_OUT.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending points confirmed and moved to available balance"),
        @ApiResponse(responseCode = "400", description = "Checkout date is in the future — points cannot be confirmed yet"),
        @ApiResponse(responseCode = "404", description = "Loyalty account not found for the given userId")
    })
    @PostMapping("/confirm-checkout")
    public ResponseEntity<String> confirmPointsAfterCheckout(
            @Valid @RequestBody AddPointsRequestDto request) {
        loyaltyService.confirmPointsAfterCheckout(request.getUserId(), request.getBookingId(), request.getAmountSpent());
        return ResponseEntity.ok("Checkout confirmed for User: " + request.getUserId());
    }

    @Operation(summary = "Cancel Points", description = "Removes pending loyalty points associated with a cancelled booking. Called internally by BookingService when a booking is cancelled.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pending points removed successfully for the cancelled booking"),
        @ApiResponse(responseCode = "404", description = "Loyalty account not found for the given userId")
    })
    @PutMapping("/cancel-points")
    public ResponseEntity<String> reduceCancelledPoints(
            @Valid @RequestBody CancelPointsRequestDto request) {
        loyaltyService.reduceCancelledBookingPoints(request.getUserId(), request.getAmount());
        return ResponseEntity.ok("Points adjusted for User Id " + request.getUserId());
    }

    @Operation(summary = "Initialize Loyalty Account", description = "Creates a new loyalty account for a user with zero points. Called internally by BookingService after user registration.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Loyalty account created successfully"),
        @ApiResponse(responseCode = "409", description = "Loyalty account already exists for this user")
    })
    @PostMapping("/initializeLoyaltyAccount/{userId}")
    public ResponseEntity<String> initializeAccount(
            @PathVariable Long userId) {
        loyaltyService.initializeAccount(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body("Profile initialized for User Id " + userId);
    }

    @Operation(summary = "Revert Redemption", description = "Restores the 300 points that were deducted during a redemption when the associated booking is cancelled. Called internally by BookingService.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "300 points refunded to the user's available balance"),
        @ApiResponse(responseCode = "404", description = "Loyalty account or redemption record not found for the given userId and bookingId")
    })
    @PostMapping("/revert-redemption/{userId}/{bookingId}")
    public ResponseEntity<String> revertRedemption(
            @PathVariable Long userId,
            @PathVariable Long bookingId) {
        loyaltyService.revertRedemption(userId, bookingId);
        return ResponseEntity.ok("Redemption reverted. Audit User: " + userId);
    }

    @Operation(
        summary = "Check Balance",
        description = "Returns the current points balance for the authenticated user. " +
                      "Requires gateway-forwarded identity headers. Only accessible by users with ROLE_GUEST.",
        parameters = {
            @Parameter(
                name = "X-User-Id",
                in = ParameterIn.HEADER,
                required = true,
                description = "User ID forwarded by the API Gateway after JWT validation",
                schema = @Schema(type = "string", example = "42")
            ),
            @Parameter(
                name = "X-User-Role",
                in = ParameterIn.HEADER,
                required = true,
                description = "User role forwarded by the API Gateway (must be ROLE_GUEST)",
                schema = @Schema(type = "string", example = "ROLE_GUEST")
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns user's current point balance"),
        @ApiResponse(responseCode = "403", description = "Access denied — role is not ROLE_GUEST"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/balance")
    public ResponseEntity<Integer> getPointsBalance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_GUEST")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(0);
        }
        return ResponseEntity.ok(loyaltyService.getPointsBalance(Long.parseLong(userIdHeader)));
    }

    @Operation(
        summary = "Redemption History",
        description = "Returns the list of past redemptions for the authenticated user. " +
                      "Requires gateway-forwarded identity headers. Only accessible by users with ROLE_GUEST.",
        parameters = {
            @Parameter(
                name = "X-User-Id",
                in = ParameterIn.HEADER,
                required = true,
                description = "User ID forwarded by the API Gateway after JWT validation",
                schema = @Schema(type = "string", example = "42")
            ),
            @Parameter(
                name = "X-User-Role",
                in = ParameterIn.HEADER,
                required = true,
                description = "User role forwarded by the API Gateway (must be ROLE_GUEST)",
                schema = @Schema(type = "string", example = "ROLE_GUEST")
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of past redemptions retrieved"),
        @ApiResponse(responseCode = "204", description = "No history found for this user"),
        @ApiResponse(responseCode = "403", description = "Access denied — role is not ROLE_GUEST")
    })
    @GetMapping("/history")
    public ResponseEntity<List<RedemptionResponseDto>> getRedemptionHistory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_GUEST")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ArrayList<>());
        }
        List<RedemptionResponseDto> history = loyaltyService.getRedemptionHistory(Long.parseLong(userIdHeader));
        return history.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(history);
    }
}