package com.cts.project.shbs.model;

import jakarta.persistence.*;
import lombok.*;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment entity representing a payment for a booking")
@Entity
@Table(name = "Payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    public enum PaymentStatus {
        SUCCESS, FAILED, REFUNDED
    }

    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, UPI, NET_BANKING, WALLET
    }

    @Schema(description = "Unique payment ID", example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    private Long paymentId;

    @Schema(description = "User ID who made the payment", example = "1")
    @Column(name = "UserID", nullable = false)
    private Long userId;

    @Schema(description = "Booking ID linked to this payment", example = "1")
    @Column(name = "BookingID")
    private Long bookingId;

    @Schema(description = "Amount paid", example = "1500.00")
    @Column(name = "Amount", nullable = false)
    private Double amount;

    @Schema(description = "Payment status", example = "SUCCESS")
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private PaymentStatus status;

    @Schema(description = "Payment method used", example = "CREDIT_CARD")
    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentMethod", length = 50)
    private PaymentMethod paymentMethod;

    @Schema(description = "Razorpay order ID", example = "order_xyz123")
    @Column(name = "RazorpayOrderId", length = 100)
    private String razorpayOrderId;

    @Schema(description = "Razorpay payment ID", example = "pay_xyz123")
    @Column(name = "RazorpayPaymentId", length = 100)
    private String razorpayPaymentId;
}