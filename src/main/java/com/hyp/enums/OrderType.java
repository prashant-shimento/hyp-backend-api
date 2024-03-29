package com.hyp.enums;

public enum OrderType {
	H, // Home Delivery
	D, // Dine In
	P; // Parcel or Take away

	public static OrderType fromCode(String code) {
		switch (code) {
		case "Delivery":
			return H;
		case "Dine In":
			return D;
		case "PickUp":
			return P;
		default:
			return H;
		}
	}
}
