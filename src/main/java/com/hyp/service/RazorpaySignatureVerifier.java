package com.hyp.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class RazorpaySignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    public boolean verifySignature(String payload, String signature, String secretKey) {
        try {
            // Calculate HMAC SHA256 hash of the payload using the API secret
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(payload.getBytes());
            String calculatedSignature = Base64.getEncoder().encodeToString(hashBytes);
            return calculatedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // Handle exceptions
            e.printStackTrace();
            return false;
        }
    }
}

