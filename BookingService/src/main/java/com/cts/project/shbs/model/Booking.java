package com.cts.project.shbs.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Booking entity representing a hotel room reservation")
@Entity
@Table(name = "Booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    public enum BookingStatus {
        CONFIRMED, CHECKED_IN, CHECKED_OUT, REVIEWED, NOT_REVIEWED, CANCELLED
    }

    @Schema(description = "Unique booking ID", example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookingID")
    private Long bookingId;

    @Schema(description = "User ID of the guest", example = "1")
    @Column(name = "UserID", nullable = false)
    private Long userId;

    @Schema(description = "Room ID being booked", example = "2")
    @Column(name = "RoomID", nullable = false)
    private Long roomId;

    @Schema(description = "Hotel ID where room belongs", example = "1")
    @Column(name = "HotelID", nullable = false)
    private Long hotelId;

    @Schema(description = "Check-in date", example = "2026-06-01")
    @Column(name = "CheckInDate", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;

    @Schema(description = "Check-out date", example = "2026-06-05")
    @Column(name = "CheckOutDate", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;

    @Schema(description = "Booking status", example = "CONFIRMED")
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private BookingStatus status;

    @Schema(description = "Payment ID linked to this booking", example = "1")
    @Column(name = "PaymentID")
    private Long paymentId;
}