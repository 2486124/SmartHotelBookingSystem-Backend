package com.cts.project.shbs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDTO {
	@NotNull(message = "User ID is required")
	@Positive(message="User ID must be a positive value")
    private Long userId;

    @NotNull(message = "Hotel ID is required")
    @Positive(message="Hotel ID must be a positive value")
    private Long hotelId;

    @NotNull
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;
    
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
}
