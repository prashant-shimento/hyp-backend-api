package com.hyp.enums;

public enum DeliveryOrderStatusType {
	CREATED, CANCELLED, PENDING, PROCESSING, FULFILLED, COMPLETED;
	
	public static DeliveryOrderStatusType getDeliveryOrderStatus(String value) {
        return switch (value) {
            case "cancelled" -> CANCELLED;
            case "pending" -> PENDING;
            case "processing" -> PROCESSING;
            case "fulfilled" -> FULFILLED;
            case "completed" -> COMPLETED;
            default -> CREATED;
        };
	}
}
