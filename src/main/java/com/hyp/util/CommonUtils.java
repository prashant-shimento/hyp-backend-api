package com.hyp.util;

import java.util.Random;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

public class CommonUtils {

	public static String genId() {
		Random random = new Random();
		char[] alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		return NanoIdUtils.randomNanoId(random, alphabet, 10);
	}

	public static String getISOAmount(double amount) {
		String orderString = Double.toString(amount);
		int indexOfDecimal = orderString.indexOf(".");
		if (indexOfDecimal != -1) {
			int decimalPlaces = orderString.length() - indexOfDecimal - 1;
			if (decimalPlaces == 1) {
				orderString = orderString.replace(".", "") + "0";
			} else {
				orderString = orderString.replace(".", "");
			}
		}
		return orderString;
	}

	public static int emptyIntToZero(String s) {
		if (s.length() == 0) {
			return 0;
		} else {
			return Integer.parseInt(s);
		}

	}

	public static String emptyIfNullOrZeroToString(Object value) {
		if (value == null || value == "" || (value instanceof Number && ((Number) value).doubleValue() == 0.0) ||
	            (value instanceof String && ((String) value).equals("0")) ||
	            (value instanceof String && ((String) value).equals("0.00"))) {
	            return "";
	        }
		return value.toString();
	}

}
