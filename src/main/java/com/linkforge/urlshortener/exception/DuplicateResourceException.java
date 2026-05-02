package com.linkforge.urlshortener.exception;

// Thrown when attempting to create a resource that already exists
public class DuplicateResourceException extends LinkForgeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
