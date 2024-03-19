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
	
}
