package com.linkforge.urlshortener.exception.resource;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when attempting to create a resource that already exists
public class DuplicateResourceException extends LinkForgeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
