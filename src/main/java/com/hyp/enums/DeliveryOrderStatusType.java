package com.hyp.enums;

public enum DeliveryOrderStatusType {
	CREATED, CANCELLED, PENDING, PROCESSING, FULFILLED, COMPLETED;
	
	public static DeliveryOrderStatusType getDeliveryOrderStatus(String value) {
		switch (value) {
		case "created":
			return CREATED;
		case "cancelled":
			return CANCELLED;
		case "pending":
			return PENDING;
		case "processing":
			return PROCESSING;
		case "fulfilled":
			return FULFILLED;
		case "completed":
			return COMPLETED;
		default:
			return CREATED;
		}
	}
}
