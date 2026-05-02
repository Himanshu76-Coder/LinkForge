package com.linkforge.urlshortener.exception;

// Thrown when a user is not authorized to perform an action
public class UnauthorizedException extends LinkForgeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
