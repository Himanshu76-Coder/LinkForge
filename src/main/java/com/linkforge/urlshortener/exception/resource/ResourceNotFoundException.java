package com.linkforge.urlshortener.exception.resource;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when a requested resource does not exist
public class ResourceNotFoundException extends LinkForgeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
