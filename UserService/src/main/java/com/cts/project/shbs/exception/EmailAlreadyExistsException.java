package com.cts.project.shbs.exception;

public class EmailAlreadyExistsException extends RuntimeException {
	public EmailAlreadyExistsException(String email) {
        super("Email is already in use: " + email);
    }
}
