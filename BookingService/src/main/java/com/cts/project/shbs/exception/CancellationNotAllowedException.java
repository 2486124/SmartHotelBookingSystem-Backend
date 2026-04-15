package com.cts.project.shbs.exception;

public class CancellationNotAllowedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CancellationNotAllowedException(String message) {
        super(message);
    }
}