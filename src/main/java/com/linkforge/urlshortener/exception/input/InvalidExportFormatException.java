package com.linkforge.urlshortener.exception.input;

import com.linkforge.urlshortener.exception.base.LinkForgeException;

// Thrown when an unsupported export format is requested (only 'json' and 'csv' are supported)
public class InvalidExportFormatException extends LinkForgeException {

    public InvalidExportFormatException(String message) {
        super(message);
    }
}
