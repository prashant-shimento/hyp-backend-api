package com.hyp.util;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtils {

    private static final DateTimeFormatter FORMATTER_WITH_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_WITHOUT_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static String genId() {
        Random random = new Random();
        char[] alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        return NanoIdUtils.randomNanoId(random, alphabet, 10);
    }

    public static String generateWorkflowId(String input) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyyHHmmssSSS"));
        String combined = input + timestamp;
        return "wf-" + shortHash(combined);
    }

    private static String shortHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) { // 4 bytes → 8 hex chars
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
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

    public static double parseISOAmount(int isoAmount) {
        return isoAmount / 100.0;
    }

    public static int emptyIntToZero(String s) {
        if (s.isEmpty()) {
            return 0;
        } else {
            return Integer.parseInt(s);
        }
    }

    public static String emptyIfNullOrZeroToString(Object value) {
        if (value == null
                || value == ""
                || (value instanceof Number && ((Number) value).doubleValue() == 0.0)
                || (value instanceof String && ((String) value).equals("0"))
                || (value instanceof String && ((String) value).equals("0.00"))) {
            return "";
        }
        return value.toString();
    }

    public static String generateReferenceId(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyHHmmss"));
        Random random = new Random();
        int identifier = random.nextInt(1000000);
        return prefix + timestamp + String.format("%04d", identifier);
    }

    public static Date getISODate(String date, String formats) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(formats);
            return dateFormat.parse(date);
        } catch (ParseException e) {
            log.error("Failed to parse date: {}", date, e);
        }
        return null;
    }

    public static String extractPincode(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }
        String pinCodePattern = "\\b\\d{6}\\b";
        Pattern pattern = Pattern.compile(pinCodePattern);
        Matcher matcher = pattern.matcher(address);

        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group();
        }
        return lastMatch;
    }

    public static boolean isToday(LocalDate localDate) {
        return LocalDate.now().equals(localDate);
    }

    public static LocalDateTime getLocalDateTimeFromString(String dateString, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(dateString, formatter);
    }

    public static List<String> buildStringList(Object... values) {
        return Arrays.stream(values).map(String::valueOf).collect(Collectors.toList());
    }

    public static long calculateTTLInSeconds(LocalDateTime turnOnTimeUTC) {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime targetUtc = turnOnTimeUTC.atZone(ZoneId.of("UTC"));

        Duration duration = Duration.between(nowUtc, targetUtc);
        long ttl = Math.max(0, duration.getSeconds());

        log.info("Auto turn-on time (UTC): {}", targetUtc);
        log.info("Current UTC time: {}", nowUtc);
        log.info("Calculated TTL (seconds): {}", ttl);

        return ttl;
    }

    public static String generateMockDeliveryOrderId() {
        String digits = String.valueOf(System.currentTimeMillis()).substring(2, 10);
        String suffix =
                UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5).toUpperCase();
        return digits + suffix;
    }

    public static LocalDateTime convertISTtoUTC(LocalDateTime istTime) {
        ZonedDateTime istZoned = istTime.atZone(ZoneId.of("Asia/Kolkata"));
        ZonedDateTime utcZoned = istZoned.withZoneSameInstant(ZoneId.of("UTC"));
        return utcZoned.toLocalDateTime();
    }

    public static LocalDateTime parseAutoTurnOnTime(String turnOnTime) {
        if (turnOnTime == null || turnOnTime.isBlank()) {
            log.error("Empty or null TurnOnTime provided.");
            return LocalDateTime.now().plusHours(2);
        }
        try {
            if (turnOnTime.length() == 19) {
                return LocalDateTime.parse(turnOnTime, FORMATTER_WITH_SECONDS);
            } else if (turnOnTime.length() == 16) {
                return LocalDateTime.parse(turnOnTime, FORMATTER_WITHOUT_SECONDS);
            } else {
                log.error("Invalid TurnOnTime format: '{}'. Expected format 'yyyy-MM-dd HH:mm[:ss]'", turnOnTime);
                return LocalDateTime.now().plusHours(2);
            }
        } catch (Exception e) {
            log.error("Failed to parse TurnOnTime '{}': {}", turnOnTime, e.getMessage(), e);
            return LocalDateTime.now().plusHours(2);
        }
    }
}
