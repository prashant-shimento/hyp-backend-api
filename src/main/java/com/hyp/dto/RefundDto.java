package com.hyp.dto;

import lombok.Data;

@Data
public class RefundDto {
	private String orderId;
	private double amount;
}
