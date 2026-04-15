package com.cts.project.shbs.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HotelGlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bodyOf(ResponseEntity<Map<String, Object>> response) {
        return response.getBody();
    }

    // ── ResourceNotFoundException → 404 ──────────────────────────────────────

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFound {

        @Test
        @DisplayName("returns 404 Not Found with correct fields")
        void returns404() {
            ResourceNotFoundException ex = new ResourceNotFoundException("Hotel not found with ID: 1");
            ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(bodyOf(response)).containsEntry("status", 404);
            assertThat(bodyOf(response)).containsEntry("error", "Not Found");
            assertThat(bodyOf(response)).containsEntry("message", "Hotel not found with ID: 1");
        }
    }

    // ── InvalidRatingException → 400 ─────────────────────────────────────────

    @Nested
    @DisplayName("InvalidRatingException")
    class InvalidRating {

        @Test
        @DisplayName("returns 400 Bad Request with correct fields")
        void returns400() {
            InvalidRatingException ex = new InvalidRatingException("Rating must be between 0.0 and 5.0. Provided: 6.0");
            ResponseEntity<Map<String, Object>> response = handler.handleInvalidRating(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(bodyOf(response)).containsEntry("status", 400);
            assertThat(bodyOf(response)).containsEntry("error", "Bad Request");
            assertThat(bodyOf(response).get("message").toString()).contains("6.0");
        }
    }

    // ── UnauthorizedAccessException → 403 ────────────────────────────────────

    @Nested
    @DisplayName("UnauthorizedAccessException")
    class UnauthorizedAccess {

        @Test
        @DisplayName("returns 403 Forbidden with correct fields")
        void returns403() {
            UnauthorizedAccessException ex = new UnauthorizedAccessException("Access denied. You can only delete your own hotel.");
            ResponseEntity<Map<String, Object>> response = handler.handleUnauthorizedAccess(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(bodyOf(response)).containsEntry("status", 403);
            assertThat(bodyOf(response)).containsEntry("error", "Forbidden");
            assertThat(bodyOf(response)).containsEntry("message", "Access denied. You can only delete your own hotel.");
        }
    }

    // ── HotelNotApprovedException → 403 ──────────────────────────────────────

    @Nested
    @DisplayName("HotelNotApprovedException")
    class HotelNotApproved {

        @Test
        @DisplayName("returns 403 with 'Hotel Not Approved' error label")
        void returns403() {
            HotelNotApprovedException ex = new HotelNotApprovedException("Hotel is not yet approved.");
            ResponseEntity<Map<String, Object>> response = handler.handleHotelNotApproved(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(bodyOf(response)).containsEntry("status", 403);
            assertThat(bodyOf(response)).containsEntry("error", "Hotel Not Approved");
            assertThat(bodyOf(response)).containsEntry("message", "Hotel is not yet approved.");
        }
    }

    // ── DuplicateResourceException → 409 ─────────────────────────────────────

    @Nested
    @DisplayName("DuplicateResourceException")
    class DuplicateResource {

        @Test
        @DisplayName("returns 409 Conflict with correct fields")
        void returns409() {
            DuplicateResourceException ex = new DuplicateResourceException("Manager already owns a hotel with ID: 1");
            ResponseEntity<Map<String, Object>> response = handler.handleDuplicateResource(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(bodyOf(response)).containsEntry("status", 409);
            assertThat(bodyOf(response)).containsEntry("error", "Conflict");
            assertThat(bodyOf(response).get("message").toString()).contains("already owns");
        }
    }

    // ── ServiceUnavailableException → 503 ────────────────────────────────────

    @Nested
    @DisplayName("ServiceUnavailableException")
    class ServiceUnavailable {

        @Test
        @DisplayName("returns 503 Service Unavailable with correct fields")
        void returns503() {
            ServiceUnavailableException ex = new ServiceUnavailableException("Downstream service is down.");
            ResponseEntity<Map<String, Object>> response = handler.handleServiceUnavailable(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(bodyOf(response)).containsEntry("status", 503);
            assertThat(bodyOf(response)).containsEntry("error", "Service Unavailable");
            assertThat(bodyOf(response)).containsEntry("message", "Downstream service is down.");
        }
    }

    // ── Generic Exception → 500 ───────────────────────────────────────────────

    @Nested
    @DisplayName("Generic Exception (fallback)")
    class GenericException {

        @Test
        @DisplayName("returns 500 with safe generic message — never exposes internal details")
        void returns500() {
            ResponseEntity<Map<String, Object>> response =
                    handler.handleGlobalException(new RuntimeException("DB connection lost"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(bodyOf(response)).containsEntry("status", 500);
            assertThat(bodyOf(response)).containsEntry("error", "Internal Server Error");
            // Must NOT leak the real exception message to the client
            assertThat(bodyOf(response).get("message")).isEqualTo("An unexpected error occurred.");
            assertThat(bodyOf(response).get("message").toString()).doesNotContain("DB connection lost");
        }
    }
}