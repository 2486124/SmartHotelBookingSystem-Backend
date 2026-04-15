package com.cts.project.shbs.dto;

import com.cts.project.shbs.model.Payment.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RazorpayOrderRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be a positive number")
    private Long userId;

    @NotNull(message = "Room ID is required")
    @Positive(message = "Room ID must be a positive number")
    private Long roomId;

    @NotNull(message = "Hotel ID is required")
    @Positive(message = "Hotel ID must be a positive number")
    private Long hotelId;

    @NotNull(message = "Check-in date is required")
    @Future(message = "Check-in date must be a future date")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out date must be a future date")
    private LocalDate checkOutDate;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}