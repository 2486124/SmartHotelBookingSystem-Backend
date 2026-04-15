package com.cts.shbsm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for requesting point additions.
 * Swagger uses @Schema to generate the "Example Value" in the UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for adding pending points to a user's loyalty account")
public class AddPointsRequestDto {
    
    @Schema(description = "The unique identifier of the user", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @Schema(description = "The ID of the booking associated with these points", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Booking ID is required for point tracking")
    private Long bookingId;

    @Schema(description = "Total monetary amount spent on the booking", example = "1500.0", minimum = "0.1")
    @NotNull(message = "Amount spent is required")
    @Positive(message = "Amount spent must be greater than zero")
    private Double amountSpent;
}