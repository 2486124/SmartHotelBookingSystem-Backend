package com.cts.project.shbs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cts.project.shbs.model.Review;
@Repository
public interface ReviewRepositoryIntf extends JpaRepository<Review,Long>{
	boolean existsByUserIdAndHotelId(Long userId, Long hotelId);
	List<Review> findByHotelId(Long hotelId);
	List<Review> findByUserId(long userId);
	@Query("SELECT AVG(r.rating) FROM Review r WHERE r.hotelId = :hotelId")
	Double findAverageRatingByHotelId(@Param("hotelId") long hotelId);
}
