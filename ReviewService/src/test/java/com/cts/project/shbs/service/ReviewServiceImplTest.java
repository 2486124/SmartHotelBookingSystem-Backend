
package com.cts.project.shbs.service;

import com.cts.project.shbs.client.BookingServiceClient;
import com.cts.project.shbs.client.HotelServiceClient;
import com.cts.project.shbs.dto.ReviewRequestDTO;
import com.cts.project.shbs.dto.ReviewResponseDTO;
import com.cts.project.shbs.exception.HotelServiceException;
import com.cts.project.shbs.exception.InvalidReviewException;
import com.cts.project.shbs.exception.ResourceNotFoundException;
import com.cts.project.shbs.model.Review;
import com.cts.project.shbs.repository.ReviewRepositoryIntf;

import feign.FeignException;
import feign.Request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceImplTest {

    @Mock
    private ReviewRepositoryIntf reviewRepositoryIntf;

    @Mock
    private HotelServiceClient hotelServiceClient;
    
    @Mock
    private BookingServiceClient bookingServiceClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private ReviewRequestDTO validRequest;
    private Review savedReview;

    @BeforeEach
    void setUp() {
        validRequest = ReviewRequestDTO.builder()
                .userId(10L)
                .hotelId(1L)
                .bookingId(200L)
                .rating(4)
                .comment("Great stay!")
                .build();

        savedReview = Review.builder()
                .reviewId(100L)
                .userId(10L)
                .hotelId(1L)
                .rating(4)
                .comment("Great stay!")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // submitReview Tests

    @Test
    void submitReview_ValidRequest_ShouldReturnResponseDTO() {
        when(reviewRepositoryIntf.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepositoryIntf.findAverageRatingByHotelId(1L)).thenReturn(4.5);
        when(hotelServiceClient.updateHotelRating(anyLong(), anyDouble()))
                .thenReturn(ResponseEntity.ok().build());
        when(bookingServiceClient.updateReviewStatus(anyLong(), anyString()))
        .thenReturn(ResponseEntity.ok("REVIEWED"));

        ReviewResponseDTO result = reviewService.submitReview(validRequest);

        assertNotNull(result);
        assertEquals(100L, result.getReviewId());
        assertEquals(1L, result.getHotelId());
        assertEquals(10L, result.getUserId());
    }

    @Test
    void submitReview_NullRequest_ShouldThrowInvalidReviewException() {
        assertThrows(InvalidReviewException.class, () -> reviewService.submitReview(null));
    }

    @Test
    void submitReview_DBError_ShouldThrowRuntimeException() {
        when(reviewRepositoryIntf.save(any(Review.class)))
                .thenThrow(new DataIntegrityViolationException("DB error"));

        assertThrows(RuntimeException.class, () -> reviewService.submitReview(validRequest));
    }

    @Test
    void submitReview_HotelNotFound_ShouldThrowResourceNotFoundException() {
        when(reviewRepositoryIntf.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepositoryIntf.findAverageRatingByHotelId(1L)).thenReturn(4.5);
        doThrow(makeFeignException(FeignException.NotFound.class, 404))
                .when(hotelServiceClient).updateHotelRating(anyLong(), anyDouble());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.submitReview(validRequest));
    }

    @Test
    void submitReview_HotelServiceDown_ShouldThrowHotelServiceException() {
        when(reviewRepositoryIntf.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepositoryIntf.findAverageRatingByHotelId(1L)).thenReturn(4.5);
        doThrow(makeFeignException(FeignException.ServiceUnavailable.class, 503))
                .when(hotelServiceClient).updateHotelRating(anyLong(), anyDouble());

        assertThrows(HotelServiceException.class, () -> reviewService.submitReview(validRequest));
    }

    // ---------------------------------------------------------------
    // getHotelReviewsByHotelId Tests
    // ---------------------------------------------------------------

    @Test
    void getHotelReviewsByHotelId_ValidId_ShouldReturnList() {
        when(reviewRepositoryIntf.findByHotelId(1L)).thenReturn(List.of(savedReview));

        List<ReviewResponseDTO> result = reviewService.getHotelReviewsByHotelId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getHotelId());
    }

    @Test
    void getHotelReviewsByHotelId_InvalidId_ShouldThrowInvalidReviewException() {
        assertThrows(InvalidReviewException.class, () -> reviewService.getHotelReviewsByHotelId(0L));
        assertThrows(InvalidReviewException.class, () -> reviewService.getHotelReviewsByHotelId(-1L));
    }

    @Test
    void getHotelReviewsByHotelId_NoReviewsFound_ShouldThrowResourceNotFoundException() {
        when(reviewRepositoryIntf.findByHotelId(1L)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.getHotelReviewsByHotelId(1L));
    }

    @Test
    void getHotelReviewsByHotelId_DBError_ShouldThrowRuntimeException() {
        when(reviewRepositoryIntf.findByHotelId(1L))
                .thenThrow(new DataIntegrityViolationException("DB error"));

        assertThrows(RuntimeException.class, () -> reviewService.getHotelReviewsByHotelId(1L));
    }

    // getReviewsByUserId Tests

    @Test
    void getReviewsByUserId_ValidId_ShouldReturnList() {
        when(reviewRepositoryIntf.findByUserId(10L)).thenReturn(List.of(savedReview));

        List<ReviewResponseDTO> result = reviewService.getReviewsByUserId(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getUserId());
    }

    @Test
    void getReviewsByUserId_InvalidId_ShouldThrowInvalidReviewException() {
        assertThrows(InvalidReviewException.class, () -> reviewService.getReviewsByUserId(0L));
        assertThrows(InvalidReviewException.class, () -> reviewService.getReviewsByUserId(-5L));
    }

    @Test
    void getReviewsByUserId_NoReviewsFound_ShouldThrowResourceNotFoundException() {
        when(reviewRepositoryIntf.findByUserId(10L)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewsByUserId(10L));
    }

    // removeReview Tests

    @Test
    void removeReview_ValidId_ShouldReturnSuccessMessage() {
        when(reviewRepositoryIntf.existsById(100L)).thenReturn(true);
        doNothing().when(reviewRepositoryIntf).deleteById(100L);

        String result = reviewService.removeReview(100L);

        assertEquals("Review Removed", result);
        verify(reviewRepositoryIntf).deleteById(100L);
    }

    @Test
    void removeReview_InvalidId_ShouldThrowInvalidReviewException() {
        assertThrows(InvalidReviewException.class, () -> reviewService.removeReview(0L));
        assertThrows(InvalidReviewException.class, () -> reviewService.removeReview(-1L));
    }

    @Test
    void removeReview_ReviewNotFound_ShouldThrowResourceNotFoundException() {
        when(reviewRepositoryIntf.existsById(100L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> reviewService.removeReview(100L));
        verify(reviewRepositoryIntf, never()).deleteById(anyLong());
    }

 // respondToReview Tests

    @Test
    void respondToReview_ValidInput_ShouldSaveAndReturnDTO() {
        when(reviewRepositoryIntf.findById(100L)).thenReturn(Optional.of(savedReview));
        when(reviewRepositoryIntf.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponseDTO result = reviewService.respondToReview(100L, "Thank you!");

        assertNotNull(result);
        assertEquals("Thank you!", result.getManagerResponse());
        assertNotNull(result.getResponseTimestamp());
    }

    @Test
    void respondToReview_InvalidId_ShouldThrowInvalidReviewException() {
        assertThrows(InvalidReviewException.class,
            () -> reviewService.respondToReview(0L, "response"));
    }

    @Test
    void respondToReview_NullResponse_ShouldThrowInvalidReviewException() {
        assertThrows(InvalidReviewException.class,
            () -> reviewService.respondToReview(100L, null));
    }

    @Test
    void respondToReview_BlankResponse_ShouldThrowInvalidReviewException() {
        assertThrows(InvalidReviewException.class,
            () -> reviewService.respondToReview(100L, "   "));
    }

    @Test
    void respondToReview_ReviewNotFound_ShouldThrowResourceNotFoundException() {
        when(reviewRepositoryIntf.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> reviewService.respondToReview(100L, "Thanks!"));
    }

    @Test
    void respondToReview_AlreadyResponded_ShouldThrowInvalidReviewException() {
        savedReview.setManagerResponse("Already replied");
        when(reviewRepositoryIntf.findById(100L)).thenReturn(Optional.of(savedReview));

        assertThrows(InvalidReviewException.class,
            () -> reviewService.respondToReview(100L, "Another reply"));
        verify(reviewRepositoryIntf, never()).save(any());
    }
    
    // getRatingsByHotelId Tests

    @Test
    void getRatingsByHotelId_ValidId_ShouldReturnRoundedRating() {
        when(reviewRepositoryIntf.findAverageRatingByHotelId(1L)).thenReturn(4.55);

        Double result = reviewService.getRatingsByHotelId(1L);

        assertEquals(4.6, result);
    }

    @Test
    void getRatingsByHotelId_NullFromDB_ShouldReturnZero() {
        when(reviewRepositoryIntf.findAverageRatingByHotelId(1L)).thenReturn(null);

        Double result = reviewService.getRatingsByHotelId(1L);

        assertEquals(0.0, result);
    }

    @Test
    void getRatingsByHotelId_InvalidId_ShouldThrowInvalidReviewException() {
        assertThrows(InvalidReviewException.class, () -> reviewService.getRatingsByHotelId(0L));
        assertThrows(InvalidReviewException.class, () -> reviewService.getRatingsByHotelId(-1L));
    }

    // Helper to create FeignException for testing

    @SuppressWarnings("unchecked")
    private <T extends FeignException> T makeFeignException(Class<T> type, int status) {
        Request dummyRequest = Request.create(
                Request.HttpMethod.GET,
                "http://hotel-service/api/hotels/1/rating",
                Map.of(), null, StandardCharsets.UTF_8, null
        );
        try {
            return type.getDeclaredConstructor(String.class, Request.class, byte[].class, Map.class)
                    .newInstance("error " + status, dummyRequest, null, Map.of());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create FeignException", e);
        }
    }
}