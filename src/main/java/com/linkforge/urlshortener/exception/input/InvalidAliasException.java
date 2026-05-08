package com.linkforge.urlshortener.exception.input;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when a custom alias contains invalid characters or matches a reserved word
public class InvalidAliasException extends LinkForgeException {

    public InvalidAliasException(String message) {
        super(message);
    }
}
