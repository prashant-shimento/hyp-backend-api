package com.hyp.security.apikey;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HmacSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${security.webhook.timestamp-tolerance:300000}")
    private long timestampTolerance; // 5 minutes default

    /**
     * Generate HMAC-SHA256 signature for a payload
     */
    public String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating HMAC signature", e);
            throw new RuntimeException("Error generating signature", e);
        }
    }

    /**
     * Validate HMAC signature for a webhook request
     */
    public boolean validateSignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }

        try {
            String expectedSignature = generateSignature(payload, secret);
            return constantTimeEquals(expectedSignature, signature);
        } catch (Exception e) {
            log.error("Error validating signature", e);
            return false;
        }
    }

    /**
     * Validate signature with timestamp to prevent replay attacks
     */
    public boolean validateSignatureWithTimestamp(String payload, String signature, String timestamp, String secret) {
        // Validate timestamp
        if (!isTimestampValid(timestamp)) {
            log.warn("Invalid or expired timestamp: {}", timestamp);
            return false;
        }

        // Validate signature (include timestamp in payload)
        String signedPayload = payload + timestamp;
        return validateSignature(signedPayload, signature, secret);
    }

    /**
     * Check if timestamp is within acceptable range
     */
    public boolean isTimestampValid(String timestamp) {
        if (timestamp == null) {
            return false;
        }

        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            long diff = Math.abs(now - ts);
            return diff <= timestampTolerance;
        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
