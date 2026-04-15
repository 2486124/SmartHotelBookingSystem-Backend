package com.cts.shbsm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Data Transfer Object used when a booking is cancelled.
 * It carries the information needed to remove the points that were previously sitting in 'Pending'.
 */
@Data
@Schema(description = "Request body for reducing/removing pending points due to a booking cancellation")
public class CancelPointsRequestDto {

    @Schema(description = "The unique identifier of the user", example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "User ID is required to identify the account")
    private Long userId;

    @Schema(description = "The original booking amount to calculate how many points to remove", example = "1500.0")
    @NotNull(message = "Cancellation amount cannot be null")
    @Positive(message = "Cancellation amount must be a positive value")
    private Double amount;
}