package com.linkforge.urlshortener.exception.auth;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when login credentials are wrong or current password does not match
public class InvalidCredentialsException extends LinkForgeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
