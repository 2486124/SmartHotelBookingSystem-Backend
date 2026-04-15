package com.cts.project.shbs.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoomRequest {

    @NotBlank(message = "Room type is required")
    private String type;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimal places")
    private BigDecimal price;

    private Boolean availability = true;

    @Size(max = 1000, message = "Features description must not exceed 1000 characters")
    private String features;

    @Pattern(
        regexp = "^(https?://).+",
        message = "Image URL must be a valid URL starting with http:// or https://"
    )
    private String imageUrl;
}