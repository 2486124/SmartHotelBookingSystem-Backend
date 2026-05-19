package com.cts.project.shbs.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.cts.project.shbs.model.Booking.BookingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEnrichedResponse {
    private Long bookingId;
    private Long userId;
    private Long roomId;
    private Long hotelId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;

    private BookingStatus status;
    private Long paymentId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // Enriched fields
    private String userName;
    private Double totalAmount;
    private String hotelName;
    private String roomType;
}
