package com.linkforge.urlshortener.exception;

// Thrown when a URL has reached its maximum allowed click count
public class ClickLimitExceededException extends LinkForgeException {

    public ClickLimitExceededException(String message) {
        super(message);
    }
}
