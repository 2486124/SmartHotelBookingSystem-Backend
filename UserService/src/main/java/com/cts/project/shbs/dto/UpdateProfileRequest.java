package com.cts.project.shbs.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Contact number is required")
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Contact number must be a valid 10-digit mobile number"
    )
    private String contactNumber;

    private String password;
}
