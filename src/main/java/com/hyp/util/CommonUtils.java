package com.hyp.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

	public static double parseISOAmount(int isoAmount) {
		return isoAmount / 100.0;
	}

	public static int emptyIntToZero(String s) {
		if (s.length() == 0) {
			return 0;
		} else {
			return Integer.parseInt(s);
		}

	}

	public static String emptyIfNullOrZeroToString(Object value) {
		if (value == null || value == "" || (value instanceof Number && ((Number) value).doubleValue() == 0.0)
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
		String referenceId = prefix + timestamp + String.format("%04d", identifier);
		return referenceId;
	}

	public static Date getISODate(String date, String formats) {
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(formats);
			return dateFormat.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		System.err.println("Failed to parse date: " + date);
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

	public static long getTtlInSeconds(String inputTime) {
		ZonedDateTime istTime = ZonedDateTime.parse(inputTime,
				DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Kolkata")));
		ZonedDateTime utcTime = istTime.withZoneSameInstant(ZoneId.of("UTC"));
		ZonedDateTime currentTimeUtc = ZonedDateTime.now(ZoneId.of("UTC"));
		Duration duration = Duration.between(currentTimeUtc, utcTime);
		return duration.getSeconds();
	}
	
	public static String generateMockDeliveryOrderId() {
		String digits = String.valueOf(System.currentTimeMillis()).substring(2, 17); 
		String suffix = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5).toUpperCase(); 
		return digits + suffix;
	}
}
