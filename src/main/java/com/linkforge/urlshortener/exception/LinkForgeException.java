package com.linkforge.urlshortener.exception;

// Base exception for all LinkForge application exceptions
public class LinkForgeException extends RuntimeException {

    public LinkForgeException(String message) {
        super(message);
    }
}
