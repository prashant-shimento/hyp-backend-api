package com.hyp.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class DeliveryException extends Exception {

    private String action;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public DeliveryException(String message) {
        super(message);
    }

    public DeliveryException(String action, String message) {
        super(message);
        this.action = action;
    }

    public DeliveryException(String action, String message, Throwable cause) {
        super(message, cause);
        this.action = action;
    }

    public DeliveryException(String action, Throwable cause) {
        super(cause);
        this.action = action;
    }
}
