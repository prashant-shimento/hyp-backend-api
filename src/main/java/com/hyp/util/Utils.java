package com.hyp.util;

import java.util.Random;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

public class Utils {

	public static String genId() {
		Random random = new Random();
		char[] alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		return NanoIdUtils.randomNanoId(random, alphabet, 10);
	}
	
}
