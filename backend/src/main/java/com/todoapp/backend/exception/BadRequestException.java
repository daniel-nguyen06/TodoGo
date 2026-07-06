package com.todoapp.backend.exception;

/**
 * Exception thrown for bad request - validation errors, business logic errors, etc (400)
 */
public class BadRequestException extends RuntimeException {

    private String errorCode;

    /**
     * Constructor with message
     * @param message the error message
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     * @param message the error message
     * @param cause the cause
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with error code and message
     * @param errorCode the error code (e.g., "PASSWORD_MISMATCH", "USERNAME_TAKEN")
     * @param message the error message
     */
    public BadRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

