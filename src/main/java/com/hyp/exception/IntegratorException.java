package com.hyp.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IntegratorException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private String vendorName;
	private String apiName;

	public IntegratorException(String vendorName, String apiName, String message) {
		super(message);
		this.vendorName = vendorName;
		this.apiName = apiName;
	}
}
