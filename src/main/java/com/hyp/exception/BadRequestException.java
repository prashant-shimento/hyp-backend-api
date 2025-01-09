package com.hyp.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BadRequestException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String entity;
	private String message;

	public BadRequestException(String entity, String message) {
        super(message);
        this.entity = entity;
        this.message = message;
    }


}
