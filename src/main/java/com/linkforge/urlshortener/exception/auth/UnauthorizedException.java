package com.linkforge.urlshortener.exception.auth;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when a user is not authorized to perform an action
public class UnauthorizedException extends LinkForgeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
