package com.cts.project.shbs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Helper to avoid repeating the map-building every time ──────────────
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "timestamp", LocalDateTime.now(),
            "status",    status.value(),
            "error",     error,
            "message",   message
        ));
    }

    // ── 404 ─────────────────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    // ── 400 ─────────────────────────────────────────────────────────────────
    @ExceptionHandler(InvalidRatingException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRating(InvalidRatingException ex) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    // ── 403 ─────────────────────────────────────────────────────────────────
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    @ExceptionHandler(HotelNotApprovedException.class)
    public ResponseEntity<Map<String, Object>> handleHotelNotApproved(HotelNotApprovedException ex) {
        return build(HttpStatus.FORBIDDEN, "Hotel Not Approved", ex.getMessage());
    }

    // ── 409 ─────────────────────────────────────────────────────────────────
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    // ── 503 ─────────────────────────────────────────────────────────────────
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage());
    }

    @ExceptionHandler(BookingServiceException.class)
    public ResponseEntity<Map<String, Object>> handleBookingService(BookingServiceException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", ex.getMessage());
    }

    // ── 500 fallback ─────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
            "An unexpected error occurred."); // never expose ex.getMessage() in production
    }
}