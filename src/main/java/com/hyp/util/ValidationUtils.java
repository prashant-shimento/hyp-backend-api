package com.hyp.util;

import java.time.LocalTime;
import java.util.List;

import com.hyp.entity.Restaurant.DeliveryHours;

public class ValidationUtils {

	public static LocalTime validateLocalTime(String timeString) {
		if (timeString == null || timeString.isEmpty()) {
			return LocalTime.MIDNIGHT;
		}
		return LocalTime.parse(timeString);
	}

	public static boolean isWithinDeliveryHours(List<DeliveryHours> deliveryHours) {
		LocalTime now = LocalTime.now();

		for (DeliveryHours deliveryHour : deliveryHours) {
			if (now.isAfter(deliveryHour.getFrom()) && now.isBefore(deliveryHour.getTo())) {
				return true;
			}
		}
		return false;
	}
}
