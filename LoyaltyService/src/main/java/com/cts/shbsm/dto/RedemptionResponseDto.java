package com.cts.shbsm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for displaying redemption details in the user's history.
 * This represents the "Result" of a past point-spending transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response body containing details of a past loyalty point redemption")
public class RedemptionResponseDto {

    @Schema(description = "Unique identifier of the redemption record", example = "5001")
    @NotNull(message = "Redemption ID must be present in the history")
    private Long redemptionId;

    @Schema(description = "ID of the booking where the discount was used", example = "101")
    @NotNull(message = "Booking ID must be present")
    private Long bookingId;

    @Schema(description = "Number of loyalty points spent", example = "500")
    @PositiveOrZero(message = "Points used cannot be negative")
    private Integer pointsUsed;

    @Schema(description = "Monetary discount amount applied to the booking", example = "50.0")
    @PositiveOrZero(message = "Discount applied cannot be negative")
    private Double discountApplied; 

    @Schema(description = "ID of the user who performed the redemption", example = "8")
    @NotNull(message = "User ID must be present")
    private Long userId;

    @Schema(description = "Current state of the redemption", example = "SUCCESS", allowableValues = {"SUCCESS", "REVERTED"})
    @NotBlank(message = "Status cannot be empty")
    private String status; 

    @Schema(description = "The date and time the points were spent")
    @PastOrPresent(message = "Redemption date cannot be in the future")
    private LocalDateTime redemptionDate;
}