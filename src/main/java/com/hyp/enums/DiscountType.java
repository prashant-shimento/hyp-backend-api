package com.hyp.enums;

public enum DiscountType {
	F, // Fixed
	P; // Percentage
	public static String fromCode(String code) {
		switch (code) {
		case "1":
			return P.toString();
		case "2":
			return F.toString();
		default:
			return F.toString();
		}
	}
}
