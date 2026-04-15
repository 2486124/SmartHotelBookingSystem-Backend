package com.cts.project.shbs.exception;

public class InvalidOrExpiredTokenException extends RuntimeException {
    public InvalidOrExpiredTokenException() {
        super("Reset link is invalid or has expired.");
    }
}