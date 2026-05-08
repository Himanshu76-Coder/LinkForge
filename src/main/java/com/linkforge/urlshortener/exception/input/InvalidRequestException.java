package com.linkforge.urlshortener.exception.input;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when the incoming request contains invalid data
public class InvalidRequestException extends LinkForgeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
