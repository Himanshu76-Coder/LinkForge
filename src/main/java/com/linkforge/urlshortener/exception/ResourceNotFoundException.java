package com.linkforge.urlshortener.exception;

// Thrown when a requested resource does not exist
public class ResourceNotFoundException extends LinkForgeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
