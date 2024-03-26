package com.hyp.exception;

public class CustomerNotFoundException extends EntityNotFoundException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CustomerNotFoundException(String message) {
		super(message);
	}

}
