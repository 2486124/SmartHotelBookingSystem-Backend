package com.cts.project.shbs.dto;

import com.cts.project.shbs.model.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = "Password must contain uppercase, lowercase, digit and special character (@$!%*?&)"
    )
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "Contact number is required")
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Contact number must be a valid 10-digit Indian mobile number"
    )
    private String contactNumber;

}