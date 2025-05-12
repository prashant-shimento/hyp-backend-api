package com.hyp.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class PaymentException extends Exception {

	private String action;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public PaymentException(String message) {
		super(message);
	}

	public PaymentException(String action, String message) {
		super(message);
		this.action = action;
	}

	public PaymentException(String action, String message, Throwable cause) {
		super(message, cause);
		this.action = action;
	}

	public PaymentException(String action, Throwable cause) {
		super(cause);
		this.action = action;
	}

}
