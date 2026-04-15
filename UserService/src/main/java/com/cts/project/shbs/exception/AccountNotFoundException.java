package com.cts.project.shbs.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String email) {
        super("No account found with email: " + email);
    }
}