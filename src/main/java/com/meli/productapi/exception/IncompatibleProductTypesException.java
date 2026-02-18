package com.meli.productapi.exception;

/**
 * Exception thrown when attempting to compare products of different types.
 */
public class IncompatibleProductTypesException extends RuntimeException {
    
    public IncompatibleProductTypesException(String message) {
        super(message);
    }

    public IncompatibleProductTypesException(String message, Throwable cause) {
        super(message, cause);
    }
}
