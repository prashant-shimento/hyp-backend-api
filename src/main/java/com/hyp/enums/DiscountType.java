package com.hyp.enums;

public enum DiscountType {
	F, // Fixed
	P; // Percentage
	public static String fromCode(String code) {
		switch (code) {
		case "1":
			return F.toString();
		case "2":
			return P.toString();
		default:
			return P.toString();
		}
	}
}
