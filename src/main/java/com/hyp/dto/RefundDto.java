package com.hyp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RefundDto {
	private String id;
	private String currency;
	private String paymentId;
	private String paymentOrderId;
	private String orderId;
	private String status;
	private Double amount;
	private String speedProcessed;
	private String speedRequested;
}
