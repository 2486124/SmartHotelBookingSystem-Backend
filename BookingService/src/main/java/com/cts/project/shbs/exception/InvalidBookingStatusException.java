package com.cts.project.shbs.exception;

public class InvalidBookingStatusException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidBookingStatusException(String message) {
        super(message);
    }
}