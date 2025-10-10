package com.hyp.exception;

public class PosException extends Exception {

    private String action;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public PosException(String message) {
        super(message);
    }

    public PosException(String action, String message) {
        super(message);
        this.setAction(action);
    }

    public PosException(String action, String message, Throwable cause) {
        super(message, cause);
        this.setAction(action);
    }

    public PosException(String action, Throwable cause) {
        super(cause);
        this.setAction(action);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
