package com.hyp.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationException extends Exception {

    private String message;

    public ValidationException(String message) {
        super(message);
        this.message = message;
    }
}
