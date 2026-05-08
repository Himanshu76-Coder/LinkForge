package com.linkforge.urlshortener.exception.base;

// Base exception for all LinkForge application exceptions
public class LinkForgeException extends RuntimeException {

    public LinkForgeException(String message) {
        super(message);
    }
}
