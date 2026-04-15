package com.cts.project.shbs.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.project.shbs.client.HotelServiceClient;
import com.cts.project.shbs.dto.HotelResponse;
import com.cts.project.shbs.dto.ReviewRequestDTO;
import com.cts.project.shbs.dto.ReviewResponseDTO;
import com.cts.project.shbs.model.Review;
import com.cts.project.shbs.service.ReviewServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Review", description = "Endpoints for managing hotel reviews")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ReviewController {

    @Autowired
    private ReviewServiceImpl reviewServiceImpl;

    @Autowired
    private HotelServiceClient hotelServiceClient;

    // ─────────────────────────────────────────────────────────────────
    // HELPER — fetches the manager's hotel and returns its hotelId
    // Throws FeignException if manager has no hotel (→ propagates as 500)
    // ─────────────────────────────────────────────────────────────────
    private Long getManagerHotelId(Long managerId) {
        HotelResponse hotel = hotelServiceClient.getHotelByManagerId(
                managerId, "ROLE_HOTEL_MANAGER");
        return hotel.getHotelId();
    }

    // ─────────────────────────────────────────────────────────────────
    // GUEST
    // ─────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Submit a review",
        description = "Allows a GUEST to submit a review for a hotel"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Review submitted successfully",
            content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "403", description = "Access denied — GUEST role required",
            content = @Content(schema = @Schema()))
    })
    @PostMapping(value = "/")
    public ResponseEntity<?> submitReview(
            @Valid @RequestBody ReviewRequestDTO reviewRequestDTO,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_GUEST")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — GUEST role required");
        }

        // Override userId from body with the trusted gateway header
        // prevents a guest submitting a review impersonating another user
        long callerId = Long.parseLong(userIdHeader);
        reviewRequestDTO.setUserId(callerId);

        log.info("REQUEST [POST /api/reviews/] - submitReview() | userId={}, hotelId={}, rating={}",
                callerId,
                reviewRequestDTO.getHotelId(),
                reviewRequestDTO.getRating());
        try {
            ReviewResponseDTO response = reviewServiceImpl.submitReview(reviewRequestDTO);
            log.info("RESPONSE [POST /api/reviews/] - Review submitted successfully | reviewId={}, hotelId={}",
                    response.getReviewId(), response.getHotelId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("ERROR [POST /api/reviews/] - Failed to submit review | userId={}, hotelId={} | Error: {}",
                    callerId, reviewRequestDTO.getHotelId(), e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GUEST, HOTEL_MANAGER, ADMIN
    // ─────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Get reviews by hotel ID",
        description = "Fetches all reviews for a specific hotel. Accessible by GUEST, HOTEL_MANAGER, ADMIN"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reviews fetched successfully",
            content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Hotel not found",
            content = @Content(schema = @Schema()))
    })
    @GetMapping(value = "/hotel/{hotelId}")
    public ResponseEntity<?> getHotelReviewsByHotelId(
            @Parameter(description = "ID of the hotel", required = true, example = "1")
            @PathVariable long hotelId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_GUEST") && !userRole.equals("ROLE_HOTEL_MANAGER") && !userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — GUEST, HOTEL_MANAGER or ADMIN role required");
        }
        log.info("REQUEST [GET /api/reviews/hotel/{}] - getHotelReviewsByHotelId()", hotelId);
        try {
            List<ReviewResponseDTO> result = reviewServiceImpl.getHotelReviewsByHotelId(hotelId);
            log.info("RESPONSE [GET /api/reviews/hotel/{}] - Fetched {} review(s)", hotelId, result.size());
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch (Exception e) {
            log.error("ERROR [GET /api/reviews/hotel/{}] - Failed to fetch reviews | Error: {}",
                    hotelId, e.getMessage(), e);
            throw e;
        }
    }

    @Operation(
        summary     = "Get average rating by hotel ID",
        description = "Returns the average rating for a hotel. Accessible by GUEST, HOTEL_MANAGER, ADMIN"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Average rating fetched successfully",
            content = @Content(schema = @Schema(implementation = Double.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Hotel not found",
            content = @Content(schema = @Schema()))
    })
    @GetMapping(value = "/hotel/{hotelId}/rating")
    public ResponseEntity<?> getRatingsByHotelId(
            @Parameter(description = "ID of the hotel", required = true, example = "1")
            @PathVariable long hotelId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_GUEST") && !userRole.equals("ROLE_HOTEL_MANAGER") && !userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — GUEST, HOTEL_MANAGER or ADMIN role required");
        }
        log.info("REQUEST [GET /api/reviews/hotel/{}/rating] - getRatingsByHotelId()", hotelId);
        try {
            Double ratings = reviewServiceImpl.getRatingsByHotelId(hotelId);
            log.info("RESPONSE [GET /api/reviews/hotel/{}/rating] - avgRating={}", hotelId, ratings);
            return ResponseEntity.status(HttpStatus.OK).body(ratings);
        } catch (Exception e) {
            log.error("ERROR [GET /api/reviews/hotel/{}/rating] - Failed to fetch rating | Error: {}",
                    hotelId, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GUEST — own reviews only
    // ─────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Get reviews by user ID",
        description = "Fetches all reviews submitted by the authenticated user. Accessible by GUEST only"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reviews fetched successfully",
            content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access denied — GUEST role required or not your data",
            content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(schema = @Schema()))
    })
    @GetMapping(value = "/user")
    public ResponseEntity<?> getReviewsByUserId(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        // GUEST can only fetch their own reviews — compare path var against gateway header
        if (!userRole.equals("ROLE_GUEST")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied");
        }
        long userId = Long.parseLong(userIdHeader);
        log.info("REQUEST [GET /api/reviews/user] - getReviewsByUserId()", userId);
        try {
            List<ReviewResponseDTO> result = reviewServiceImpl.getReviewsByUserId(userId);
            log.info("RESPONSE [GET /api/reviews/user] - Fetched {} review(s)", userId, result.size());
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch (Exception e) {
            log.error("ERROR [GET /api/reviews/user] - Failed to fetch reviews | Error: {}",
                    userId, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // HOTEL_MANAGER — own hotel reviews only
    // ─────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Respond to a review",
        description = "Allows a HOTEL_MANAGER to post a response to a review on their own hotel"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Manager response recorded successfully",
            content = @Content(schema = @Schema(implementation = ReviewResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Access denied — HOTEL_MANAGER role required or not your hotel",
            content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Review not found",
            content = @Content(schema = @Schema()))
    })
    @PatchMapping(value = "/{reviewId}/respond")
    public ResponseEntity<?> respondToReview(
            @Parameter(description = "ID of the review to respond to", required = true, example = "5")
            @PathVariable long reviewId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Manager response body",
                required = true,
                content = @Content(schema = @Schema(example = "{\"managerResponse\": \"Thank you for your feedback!\"}"))
            )
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {

        if (!userRole.equals("ROLE_HOTEL_MANAGER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — HOTEL_MANAGER role required");
        }

        // Step 1 — get this manager's hotelId via Feign
        Long callerId = Long.parseLong(userIdHeader);
        Long managerHotelId = getManagerHotelId(callerId);

        // Step 2 — get the review and verify it belongs to their hotel
        Review review = reviewServiceImpl.getReviewById(reviewId);
        if (!review.getHotelId().equals(managerHotelId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — this review does not belong to your hotel");
        }

        // Step 3 — safe to respond
        log.info("REQUEST [PATCH /api/reviews/{}/respond] - respondToReview() | HOTEL_MANAGER responding", reviewId);
        try {
            String managerResponse = body.get("managerResponse");
            log.debug("DEBUG [PATCH /api/reviews/{}/respond] - managerResponse length={}",
                    reviewId, managerResponse != null ? managerResponse.length() : 0);
            ReviewResponseDTO response = reviewServiceImpl.respondToReview(reviewId, managerResponse);
            log.info("RESPONSE [PATCH /api/reviews/{}/respond] - Manager response recorded successfully", reviewId);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("ERROR [PATCH /api/reviews/{}/respond] - Failed to respond to review | Error: {}",
                    reviewId, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Remove a review",
        description = "Allows ADMIN to permanently delete a review by its ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review removed successfully",
            content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required",
            content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Review not found",
            content = @Content(schema = @Schema()))
    })
    @DeleteMapping(value = "/{reviewId}/remove")
    public ResponseEntity<?> removeReviewByAdmin(
            @Parameter(description = "ID of the review to remove", required = true, example = "5")
            @PathVariable long reviewId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied — ADMIN role required");
        }
        log.warn("REQUEST [DELETE /api/reviews/{}/remove] - removeReviewByAdmin() | ADMIN action triggered", reviewId);
        try {
            Object result = reviewServiceImpl.removeReview(reviewId);
            log.info("RESPONSE [DELETE /api/reviews/{}/remove] - Review removed successfully by ADMIN", reviewId);
            return ResponseEntity.status(HttpStatus.OK).body(result);
        } catch (Exception e) {
            log.error("ERROR [DELETE /api/reviews/{}/remove] - Failed to remove review | Error: {}",
                    reviewId, e.getMessage(), e);
            throw e;
        }
    }
}