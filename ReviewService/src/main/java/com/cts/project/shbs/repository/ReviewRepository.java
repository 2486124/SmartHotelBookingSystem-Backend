package com.cts.project.shbs.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.cts.project.shbs.model.Review;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByHotelId(Long hotelId);

    List<Review> findByUserId(Long userId);

    boolean existsByUserIdAndHotelId(Long userId, Long hotelId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.hotelId = :hotelId")
    Double findAverageRatingByHotelId(Long hotelId);
}