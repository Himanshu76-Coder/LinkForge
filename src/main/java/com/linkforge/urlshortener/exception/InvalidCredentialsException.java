package com.linkforge.urlshortener.exception;

// Thrown when login credentials are wrong or current password does not match
public class InvalidCredentialsException extends LinkForgeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
