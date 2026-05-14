package com.cts.project.shbs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingNotificationDto {
    private Long userId;
    private Long bookingId;
    private String hotelName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Double amount;
    private String paymentMethod;
    private boolean redeemPoints;
    private String type;           // "CONFIRMED" or "CANCELLED"
    private Double refundAmount;   // only for cancellations
}
