package com.cts.shbsm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LoyaltyID")
    private Long loyaltyId;

    @Column(name = "UserID", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "pendingPoints")
    private Integer pendingPoints;
    
    @Column(name = "PointsBalance")
    private Integer pointsBalance;

    @Column(name = "LastUpdated")
    private LocalDateTime lastUpdated;

    /**
     * Optional: Automatically update the timestamp before saving to DB
     */
//    @PrePersist
//    @PreUpdate
//    public void updateTimestamp() {
//        this.lastUpdated = LocalDateTime.now();
//    }
}