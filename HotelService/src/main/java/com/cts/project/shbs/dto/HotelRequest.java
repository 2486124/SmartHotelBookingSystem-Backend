package com.cts.project.shbs.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class HotelRequest {

    @NotBlank(message = "Hotel name is required")
    @Size(min = 2, max = 200, message = "Hotel name must be between 2 and 200 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 500, message = "Location must be between 2 and 500 characters")
    private String location;

    @Size(max = 1000, message = "Amenities description must not exceed 1000 characters")
    private String amenities;

    @Pattern(
        regexp = "^(https?://).+",
        message = "Image URL must be a valid URL starting with http:// or https://"
    )
    private String imageUrl;
}