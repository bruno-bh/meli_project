package com.meli.productapi.exception;

/**
 * Exception lançada quando tenta-se comparar produtos de tipos diferentes
 */
public class IncompatibleProductTypesException extends RuntimeException {
    
    public IncompatibleProductTypesException(String message) {
        super(message);
    }

    public IncompatibleProductTypesException(String message, Throwable cause) {
        super(message, cause);
    }
}
