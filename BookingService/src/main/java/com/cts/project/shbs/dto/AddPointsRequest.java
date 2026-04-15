package com.cts.project.shbs.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPointsRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @Positive(message = "Amount spent must be greater than zero")
    private double amountSpent;
    
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
}