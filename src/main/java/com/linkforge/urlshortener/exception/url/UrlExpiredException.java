package com.linkforge.urlshortener.exception.url;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when a URL has passed its expiration date
public class UrlExpiredException extends LinkForgeException {

    public UrlExpiredException(String message) {
        super(message);
    }
}
