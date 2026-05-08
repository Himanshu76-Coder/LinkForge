package com.linkforge.urlshortener.exception.url;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when attempting to redirect a URL that has been deactivated by its owner
public class LinkNotActiveException extends LinkForgeException {

    public LinkNotActiveException(String message) {
        super(message);
    }
}
