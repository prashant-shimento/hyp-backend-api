package com.hyp.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

public class EncryptionUtils {

	private static final String ALGORITHM = "AES";
	private static byte[] KEY;

	public static void setKey(String appSecretKey) {
		KEY = appSecretKey.getBytes();
	}

	public static String encrypt(String value) throws Exception {
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		SecretKey secretKey = new SecretKeySpec(KEY, ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] encryptedValue = cipher.doFinal(value.getBytes());
		return Base64.getEncoder().encodeToString(encryptedValue);
	}

	public static String decrypt(String value) throws Exception {
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		SecretKey secretKey = new SecretKeySpec(KEY, ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		byte[] decryptedValue = cipher.doFinal(Base64.getDecoder().decode(value));
		return new String(decryptedValue);
	}
}
