package com.hyp.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderType {
	H("1"), // Home Delivery
	D("2"), // Dine In
	P("3"); // Parcel or Take away

	private final String code;

	public static OrderType fromCode(String code) {
		for (OrderType type : OrderType.values()) {
			if (type.code.equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown OrderType code: " + code);
	}
}