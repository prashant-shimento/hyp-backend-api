package com.hyp.enums;

public enum PosOrderStatusType {
	CANCELLED(-1), ACCEPTED(1), IN_PROGRESS(2), READY(3), DISPATCHED(4), FOOD_READY(5), DELIVERED(10);

	private final int value;

	PosOrderStatusType(int value) {
        this.value = value;
    }

	public int getValue() {
		return value;
	}
}
