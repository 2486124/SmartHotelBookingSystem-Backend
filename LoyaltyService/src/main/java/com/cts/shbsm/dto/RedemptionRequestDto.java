package com.cts.shbsm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for processing point redemptions.
 * Used when a user wants to apply their loyalty points for a discount on a booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for redeeming loyalty points for a booking discount")
public class RedemptionRequestDto {

    @Schema(description = "The unique identifier of the user", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "User ID is required for redemption")
    private Long userId;

    @Schema(description = "The unique ID of the booking where the discount is applied", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Booking ID is required to link the redemption")
    private Long bookingId;

    @Schema(description = "the amount spent on booking")
    @NotNull(message = "amount is needed to apply discount")
    private Double totalAmount;
}