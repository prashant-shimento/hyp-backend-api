package com.hyp.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class OneSignalException extends Exception {

    private String action;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public OneSignalException(String message) {
        super(message);
    }

    public OneSignalException(String action, String message) {
        super(message);
        this.action = action;
    }

    public OneSignalException(String action, String message, Throwable cause) {
        super(message, cause);
        this.action = action;
    }

    public OneSignalException(String action, Throwable cause) {
        super(cause);
        this.action = action;
    }
}
