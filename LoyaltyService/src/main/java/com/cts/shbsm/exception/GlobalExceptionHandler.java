package com.cts.shbsm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import com.cts.shbsm.dto.ErrorDetailsDto;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle Custom Resource Not Found (HTTP 404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDetailsDto> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorDetailsDto errorDetails = new ErrorDetailsDto(LocalDateTime.now(), ex.getMessage(), 
                request.getDescription(false), HttpStatus.NOT_FOUND.value());
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    // 2. Handle Custom Business Logic Errors (HTTP 400)
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorDetailsDto> handleInsufficientBalance(InsufficientBalanceException ex, WebRequest request) {
        ErrorDetailsDto errorDetails = new ErrorDetailsDto(LocalDateTime.now(), ex.getMessage(), 
                request.getDescription(false), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    // 3. Handle Bean Validation Errors (HTTP 400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // 4. Global Fallback for any other Exception (HTTP 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetailsDto> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetailsDto errorDetails = new ErrorDetailsDto(LocalDateTime.now(), "An unexpected error occurred", 
                ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}