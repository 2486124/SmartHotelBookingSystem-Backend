package com.cts.project.shbs.model;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="ReviewId")
    private Long reviewId;

    @Column(name="UserId",nullable = false)
    private Long userId;

    @Column(name="HotelId",nullable = false)
    private Long hotelId;

    @Column(name="Rating",nullable = false)
    private Integer rating;         // 1–5

    @Column(name="Comment",length = 1000)
    private String comment;

    @Column(name="CommentTimeStamp",updatable = false)
    private LocalDateTime timestamp;

    @Column(name="Reply",length = 1000)
    private String managerResponse;
    @Column(name="ReplyTimeStamp")
    private LocalDateTime responseTimestamp;

    // PENDING | APPROVED | REJECTED


    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}

