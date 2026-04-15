package com.cts.shbsm.model;
import jakarta.persistence.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "redemption")
@Data
public class Redemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RedemptionID")
    private Long redemptionId;

    @Column(name = "UserID")
    private Long userId;

    @Column(name = "BookingID", unique=true)
    private Long bookingId;

    @Column(name = "PointsUsed")
    private Integer pointsUsed;

    @Column(name = "DiscountAmount")
    private Double discountAmount;

    @Column(name = "RedemptionDate")
    private LocalDateTime redemptionDate; // Java 8+ Date type
}