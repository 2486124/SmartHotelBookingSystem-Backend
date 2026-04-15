package com.cts.project.shbs.dto;

import com.cts.project.shbs.model.Payment.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private Double amount;
    private String currency;
    private String keyId;
    private Long userId;
    private Long roomId;
    private Long hotelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private PaymentMethod paymentMethod;
}