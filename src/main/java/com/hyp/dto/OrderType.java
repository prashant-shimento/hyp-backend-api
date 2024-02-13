package com.hyp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderType {
	private int orderTypeId;
	private String orderType;
}
