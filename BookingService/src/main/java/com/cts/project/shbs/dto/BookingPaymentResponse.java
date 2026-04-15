package com.cts.project.shbs.dto;

import com.cts.project.shbs.model.Booking.BookingStatus;
import com.cts.project.shbs.model.Payment.PaymentMethod;
import com.cts.project.shbs.model.Payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentResponse {
    private Long bookingId;
    private Long userId;
    private Long roomId;
    private Long hotelId;
    private BookingStatus bookingStatus;
    private Long paymentId;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private Double amount;
    private String razorpayOrderId;
    private String razorpayPaymentId;
}