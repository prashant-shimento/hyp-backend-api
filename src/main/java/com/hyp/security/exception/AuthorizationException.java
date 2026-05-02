package com.hyp.security.exception;

public class AuthorizationException extends RuntimeException {

    private final String errorCode;

    public AuthorizationException(String message) {
        super(message);
        this.errorCode = "ACCESS_DENIED";
    }

    public AuthorizationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ACCESS_DENIED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
