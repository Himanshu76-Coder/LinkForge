package com.linkforge.urlshortener.exception.auth;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when a JWT access token or refresh token is invalid, expired, or revoked
public class TokenException extends LinkForgeException {

    public TokenException(String message) {
        super(message);
    }
}
