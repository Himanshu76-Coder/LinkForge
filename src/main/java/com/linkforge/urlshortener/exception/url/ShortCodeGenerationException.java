package com.linkforge.urlshortener.exception.url;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when all retry attempts to generate a unique short code are exhausted
public class ShortCodeGenerationException extends LinkForgeException {

    public ShortCodeGenerationException(String message) {
        super(message);
    }
}
