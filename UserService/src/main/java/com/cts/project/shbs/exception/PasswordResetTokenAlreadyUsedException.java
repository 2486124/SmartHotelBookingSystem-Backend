package com.cts.project.shbs.exception;

public class PasswordResetTokenAlreadyUsedException extends RuntimeException {
    public PasswordResetTokenAlreadyUsedException() {
        super("Reset link has already been used.");
    }
}