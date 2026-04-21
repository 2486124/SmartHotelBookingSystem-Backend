package com.cts.project.shbs.exception;

public class BookingServiceException extends RuntimeException {
    public BookingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}