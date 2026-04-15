package com.cts.project.shbs.service;

import java.util.List;
import java.util.Map;

import com.cts.project.shbs.dto.ReviewRequestDTO;
import com.cts.project.shbs.dto.ReviewResponseDTO;

public interface ReviewServiceIntf {
	 ReviewResponseDTO submitReview(ReviewRequestDTO reviewRequestDTO);
	 List<ReviewResponseDTO> getHotelReviewsByHotelId(long hotelId);
	 List<ReviewResponseDTO> getReviewsByUserId(long userId);
	 String removeReview(long reviewId);
	 Double getRatingsByHotelId(long hotelId);
	 ReviewResponseDTO respondToReview(long reviewId, String managerResponse);
}
