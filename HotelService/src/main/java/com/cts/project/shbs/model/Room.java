package com.cts.project.shbs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "room")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean availability = true;

    @Column(columnDefinition = "TEXT")
    private String features;

    private String imageUrl;

    // Many Rooms belong to One Hotel
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotelId", nullable = false)
    @JsonIgnore // Prevents infinite recursion when fetching JSON
    private Hotel hotel;
}
