package com.todoapp.backend.exception;

/**
 * Exception thrown when user tries to access/modify a resource they don't own (403)
 */
public class AccessDeniedException extends RuntimeException {

    /**
     * Constructor with message
     * @param message the error message
     */
    public AccessDeniedException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     * @param message the error message
     * @param cause the cause
     */
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}

