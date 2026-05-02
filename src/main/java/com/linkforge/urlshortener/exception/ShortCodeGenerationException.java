package com.linkforge.urlshortener.exception;

// Thrown when all retry attempts to generate a unique short code are exhausted
public class ShortCodeGenerationException extends LinkForgeException {

    public ShortCodeGenerationException(String message) {
        super(message);
    }
}
