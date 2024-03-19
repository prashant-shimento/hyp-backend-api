package com.hyp.enums;

public enum OrderStatusType {
	CREATED, CONFIRMED, PAYMENT_PENDING, PAID, PAYMENT_FAILED, PROCESSING, DISPATCHED, READY, DELIVERED, CANCELLED,
	ERROR;

	public static OrderStatusType getOrderStatusByPosStatus(String value) {
		switch (value) {
		case "-1":
			return CANCELLED;
		case "4":
			return DISPATCHED;
		case "5":
			return READY;
		case "10":
			return DELIVERED;
		default:
			return PROCESSING;
		}
	}
}
