package com.cts.project.shbs.service;

import java.time.LocalDateTime;
import feign.FeignException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.project.shbs.client.BookingServiceClient;
import com.cts.project.shbs.client.HotelServiceClient;
import com.cts.project.shbs.dto.ReviewRequestDTO;
import com.cts.project.shbs.dto.ReviewResponseDTO;
import com.cts.project.shbs.exception.HotelServiceException;
import com.cts.project.shbs.exception.InvalidReviewException;
import com.cts.project.shbs.exception.ResourceNotFoundException;
import com.cts.project.shbs.model.Review;
import com.cts.project.shbs.repository.ReviewRepositoryIntf;

@Service
public class ReviewServiceImpl implements ReviewServiceIntf {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    @Autowired
    private ReviewRepositoryIntf reviewRepositoryIntf;

    @Autowired
    private HotelServiceClient hotelServiceClient;

    @Autowired
    private BookingServiceClient bookingServiceClient;

    @Override
    @Transactional
    public ReviewResponseDTO submitReview(ReviewRequestDTO reviewRequestDTO) {

        if (reviewRequestDTO == null) {
            logger.error("submitReview() - ReviewRequestDTO is null");
            throw new InvalidReviewException("Review request cannot be null");
        }
        logger.debug("submitReview() - Validation passed | userId={}, hotelId={}, rating={}, comment={}",
                reviewRequestDTO.getUserId(),
                reviewRequestDTO.getHotelId(),
                reviewRequestDTO.getRating(),
                reviewRequestDTO.getComment());

        Review review;
        try {
            Review entity = Review.builder()
                    .userId(reviewRequestDTO.getUserId())
                    .hotelId(reviewRequestDTO.getHotelId())
                    .rating(reviewRequestDTO.getRating())
                    .comment(reviewRequestDTO.getComment())
                    .build();

            review = reviewRepositoryIntf.save(entity);
            logger.info("submitReview() - Review persisted | reviewId={}", review.getReviewId());

        } catch (DataAccessException ex) {
            logger.error("submitReview() - DB error while saving review | userId={}, hotelId={}",
                    reviewRequestDTO.getUserId(), reviewRequestDTO.getHotelId(), ex);
            throw new RuntimeException("Failed to save review due to a database error", ex);
        }

        // Calculate average rating
        Double avgRating = getRatingsByHotelId(review.getHotelId());
        logger.debug("submitReview() - avgRating={} for hotelId={}", avgRating, review.getHotelId());

        // Update hotel rating via Feign — review is NOT rolled back if this fails
        try {
            ResponseEntity<Void> hotelRatingResponse =
                    hotelServiceClient.updateHotelRating(review.getHotelId(), avgRating);

            if (hotelRatingResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("submitReview() - Hotel rating updated | hotelId={}, newAvgRating={}",
                        review.getHotelId(), avgRating);
            } else {
                logger.warn("submitReview() - Hotel rating NOT updated (fallback) | hotelId={} | status={}",
                        review.getHotelId(), hotelRatingResponse.getStatusCode());
            }

        } catch (FeignException.NotFound e) {
            logger.error("submitReview() - Hotel not found | hotelId={}", review.getHotelId());
            throw new ResourceNotFoundException("Hotel not found with ID: " + review.getHotelId());
        } catch (FeignException.ServiceUnavailable | FeignException.InternalServerError e) {
            logger.error("submitReview() - Hotel service unavailable | hotelId={}", review.getHotelId());
            throw new HotelServiceException("Hotel service is currently unavailable");
        }

        // Mark booking as REVIEWED via Feign
        ResponseEntity<String> bookingResponse = bookingServiceClient.updateReviewStatus(
                reviewRequestDTO.getBookingId(), "REVIEWED");

        if (bookingResponse.getStatusCode().is2xxSuccessful()) {
            logger.info("submitReview() - Booking ID: {} marked as REVIEWED",
                    reviewRequestDTO.getBookingId());
        } else {
            logger.warn("submitReview() - Could not mark booking as REVIEWED | bookingId={} | status={}",
                    reviewRequestDTO.getBookingId(), bookingResponse.getStatusCode());
        }

        return toDTO(review);
    }

    @Override
    public List<ReviewResponseDTO> getHotelReviewsByHotelId(long hotelId) {

        if (hotelId <= 0) {
            logger.error("getHotelReviewsByHotelId() - Invalid hotelId={}", hotelId);
            throw new InvalidReviewException("Hotel ID must be a positive value");
        }

        logger.debug("getHotelReviewsByHotelId() - Querying reviews for hotelId={}", hotelId);

        List<Review> result;
        try {
            result = reviewRepositoryIntf.findByHotelId(hotelId);
        } catch (DataAccessException ex) {
            logger.error("getHotelReviewsByHotelId() - DB error for hotelId={}", hotelId, ex);
            throw new RuntimeException("Failed to fetch reviews due to a database error", ex);
        }

        if (result.isEmpty()) {
            logger.warn("getHotelReviewsByHotelId() - No reviews found for hotelId={}", hotelId);
            throw new ResourceNotFoundException("No reviews found for hotel id: " + hotelId);
        }

        logger.info("getHotelReviewsByHotelId() - Found {} review(s) for hotelId={}", result.size(), hotelId);
        return result.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<ReviewResponseDTO> getReviewsByUserId(long userId) {

        if (userId <= 0) {
            logger.error("getReviewsByUserId() - Invalid userId={}", userId);
            throw new InvalidReviewException("User ID must be a positive value");
        }

        logger.debug("getReviewsByUserId() - Querying reviews for userId={}", userId);

        List<Review> result;
        try {
            result = reviewRepositoryIntf.findByUserId(userId);
        } catch (DataAccessException ex) {
            logger.error("getReviewsByUserId() - DB error for userId={}", userId, ex);
            throw new RuntimeException("Failed to fetch reviews due to a database error", ex);
        }

        if (result.isEmpty()) {
            logger.warn("getReviewsByUserId() - No reviews found for userId={}", userId);
            throw new ResourceNotFoundException("No reviews found for user id: " + userId);
        }

        logger.info("getReviewsByUserId() - Found {} review(s) for userId={}", result.size(), userId);
        return result.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public String removeReview(long reviewId) {

        if (reviewId <= 0) {
            logger.error("removeReview() - Invalid reviewId={}", reviewId);
            throw new InvalidReviewException("Review ID must be a positive value");
        }

        if (!reviewRepositoryIntf.existsById(reviewId)) {
            logger.error("removeReview() - Review not found | reviewId={}", reviewId);
            throw new ResourceNotFoundException("Review not found with id: " + reviewId);
        }

        logger.warn("removeReview() - Deleting review | reviewId={}", reviewId);

        try {
            reviewRepositoryIntf.deleteById(reviewId);
        } catch (DataAccessException ex) {
            logger.error("removeReview() - DB error while deleting reviewId={}", reviewId, ex);
            throw new RuntimeException("Failed to delete review due to a database error", ex);
        }

        logger.info("removeReview() - Review deleted successfully | reviewId={}", reviewId);
        return "Review Removed";
    }

    // Ownership check (Feign call + hotelId comparison) is done in the controller
    // Service receives only the validated reviewId and managerResponse
    @Override
    @Transactional
    public ReviewResponseDTO respondToReview(long reviewId, String managerResponse) {

        if (reviewId <= 0) {
            logger.error("respondToReview() - Invalid reviewId={}", reviewId);
            throw new InvalidReviewException("Review ID must be a positive value");
        }
        if (managerResponse == null || managerResponse.trim().isEmpty()) {
            logger.error("respondToReview() - Empty manager response for reviewId={}", reviewId);
            throw new InvalidReviewException("Manager response cannot be null or empty");
        }

        logger.debug("respondToReview() - Fetching review | reviewId={}", reviewId);

        Review review = reviewRepositoryIntf.findById(reviewId).orElseThrow(() -> {
            logger.error("respondToReview() - Review not found | reviewId={}", reviewId);
            return new ResourceNotFoundException("Review not found with id: " + reviewId);
        });

        // Prevent overwriting an existing response
        if (review.getManagerResponse() != null && !review.getManagerResponse().trim().isEmpty()) {
            logger.warn("respondToReview() - Response already exists for reviewId={}", reviewId);
            throw new InvalidReviewException("A response has already been submitted for review id: " + reviewId);
        }

        review.setManagerResponse(managerResponse.trim());
        review.setResponseTimestamp(LocalDateTime.now());

        Review saved;
        try {
            saved = reviewRepositoryIntf.save(review);
        } catch (DataAccessException ex) {
            logger.error("respondToReview() - DB error while saving response for reviewId={}", reviewId, ex);
            throw new RuntimeException("Failed to save manager response due to a database error", ex);
        }

        logger.info("respondToReview() - Response saved | reviewId={}, responseTimestamp={}",
                saved.getReviewId(), saved.getResponseTimestamp());

        return toDTO(saved);
    }

    @Override
    public Double getRatingsByHotelId(long hotelId) {

        if (hotelId <= 0) {
            logger.error("getRatingsByHotelId() - Invalid hotelId={}", hotelId);
            throw new InvalidReviewException("Hotel ID must be a positive value");
        }

        logger.debug("getRatingsByHotelId() - Fetching average rating for hotelId={}", hotelId);

        Double rating;
        try {
            rating = reviewRepositoryIntf.findAverageRatingByHotelId(hotelId);
        } catch (DataAccessException ex) {
            logger.error("getRatingsByHotelId() - DB error for hotelId={}", hotelId, ex);
            throw new RuntimeException("Failed to calculate average rating due to a database error", ex);
        }

        Double rounded = rating != null ? Math.round(rating * 10.0) / 10.0 : 0.0;
        logger.info("getRatingsByHotelId() - hotelId={} | rawRating={} | roundedRating={}", hotelId, rating, rounded);
        return rounded;
    }

    public Review getReviewById(long reviewId) {
        return reviewRepositoryIntf.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
    }

    private ReviewResponseDTO toDTO(Review review) {
        logger.debug("toDTO() - Mapping Review to DTO | reviewId={}", review.getReviewId());
        return ReviewResponseDTO.builder()
                .reviewId(review.getReviewId())
                .userId(review.getUserId())
                .hotelId(review.getHotelId())
                .rating(review.getRating())
                .comment(review.getComment())
                .timestamp(review.getTimestamp())
                .managerResponse(review.getManagerResponse())
                .responseTimestamp(review.getResponseTimestamp())
                .build();
    }
}