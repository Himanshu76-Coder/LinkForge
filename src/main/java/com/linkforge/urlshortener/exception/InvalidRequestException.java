package com.linkforge.urlshortener.exception;

// Thrown when the incoming request contains invalid data
public class InvalidRequestException extends LinkForgeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
