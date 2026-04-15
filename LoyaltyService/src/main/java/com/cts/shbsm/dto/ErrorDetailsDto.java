package com.cts.shbsm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Standardized DTO for sending error responses across the Eat-ezy application.
 * This ensures that every API error has a consistent structure.
 */
@Data
@AllArgsConstructor
@Schema(description = "Standard structure for all API error responses")
public class ErrorDetailsDto {

    @Schema(description = "The exact time the error occurred", example = "2026-04-07T16:50:00")
    private LocalDateTime timestamp;

    @Schema(description = "A short, user-friendly summary of the error", example = "Insufficient points balance")
    private String message;

    @Schema(description = "Detailed description of the error or the request path", example = "uri=/api/loyalty/redeem")
    private String details;

    @Schema(description = "The HTTP status code", example = "400")
    private int status;
}