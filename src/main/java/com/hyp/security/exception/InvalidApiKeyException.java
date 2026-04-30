package com.hyp.security.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidApiKeyException extends AuthenticationException {

    public InvalidApiKeyException(String message) {
        super(message);
    }

    public InvalidApiKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
