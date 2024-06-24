package com.hyp.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.hyp.entity.Restaurant.DeliveryHours;

public class ValidationUtils {

	public static String validateTimeString(String timeString) {
		if (timeString == null || timeString.isEmpty()) {
			return "00:00";
		}
		try {
			LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));
			return timeString;
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("Invalid time format, expected HH:mm");
		}
	}

	public static boolean isWithinDeliveryHours(List<DeliveryHours> deliveryHours) {
		LocalTime now = LocalTime.now();

		for (DeliveryHours deliveryHour : deliveryHours) {
			LocalTime from = LocalTime.parse(deliveryHour.getFrom());
			LocalTime to = LocalTime.parse(deliveryHour.getTo());

			if (now.isAfter(from) && now.isBefore(to)) {
				return true;
			}
		}
		return false;
	}
}
