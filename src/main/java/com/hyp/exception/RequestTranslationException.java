package com.hyp.exception;

public class RequestTranslationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RequestTranslationException(String message, String vendor) {
		super("Exception occurred while translating request for vendor " + vendor + ": " + message);
	}

}
