package com.cts.project.shbs.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import feign.FeignException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 404,
            "error", "Not Found",
            "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", 400,
            "error", "Bad Request",
            "message", ex.getMessage()
        ));
    }
    @ExceptionHandler(HotelServiceException.class)
    public ResponseEntity<Map<String,Object>> handleHotelException(HotelServiceException ex){
    	return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
    			"timestamp",LocalDateTime.now().toString(),
    			"status",HttpStatus.SERVICE_UNAVAILABLE.value(),
    			"error","Hotel communication Exception",
    			"message",ex.getMessage()
    			));
    }
    @ExceptionHandler(InvalidReviewException.class)
    public ResponseEntity<Map<String,Object>> handleInvalidReview(InvalidReviewException ex){
    	
    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
    			"timestamp",LocalDateTime.now().toString(),
    			"status",HttpStatus.BAD_REQUEST.value(),
    			"error","Review Invalid Exception",
    			"message",ex.getMessage()
    			));
    }
   //bean validation failure
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));   
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
    			"timestamp",LocalDateTime.now().toString(),
    			"status",HttpStatus.BAD_REQUEST.value(),
    			"error","Validation Failed",
    			"message",errors
    			));
    }
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Map<String,Object>> handleFeignNotFoundException(FeignException.NotFound ex){
    	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
    			"timestamp",LocalDateTime.now().toString(),
    			"status",HttpStatus.BAD_REQUEST.value(),
    			"error","Feign Client Exception",
    			"message",ex.getMessage()
    			));
    }
    @ExceptionHandler(FeignException.ServiceUnavailable.class)
    public ResponseEntity<Map<String,Object>>handleFeignServiceUnAvailableException(FeignException.ServiceUnavailable ex){
    	return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
    			"timestamp",LocalDateTime.now().toString(),
    			"status",HttpStatus.SERVICE_UNAVAILABLE.value(),
    			"error","Feign Client Service Unavailable",
    			"message",ex.getMessage()
    			));
    }
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String,Object>> handleFeignException(FeignException ex){
    	return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
    			"timestamp",LocalDateTime.now().toString(),
    			"status",HttpStatus.BAD_GATEWAY.value(),
    			"error","Error comunicating with Service using Feign Client",
    			"message",ex.getMessage()
    			));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String,Object>>handleRuntimeException(RuntimeException ex){
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
    			"timestamp",LocalDateTime.now().toString(),
    			"status",HttpStatus.INTERNAL_SERVER_ERROR.value(),
    			"error","An unexpected Error occurs, please try again later",
    			"message",ex.getMessage()
    			));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>>handleException(Exception ex){
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
    			"timestamp",LocalDateTime.now().toString(),
    			"status",HttpStatus.INTERNAL_SERVER_ERROR.value(),
    			"error","Internal server occured",
    			"message",ex.getMessage()
    			));
    }
}
