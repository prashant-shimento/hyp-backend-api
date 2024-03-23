package com.hyp.dto;

import lombok.Data;

@Data
public class VerificationRequestDto {
	private String mobile;
	private int otp;
}
