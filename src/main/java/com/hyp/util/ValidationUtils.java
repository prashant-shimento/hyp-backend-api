package com.hyp.util;

import com.hyp.entity.Restaurant.DeliveryHours;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

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
        ZonedDateTime nowUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime nowIST = nowUTC.withZoneSameInstant(ZoneId.of("Asia/Kolkata"));
        LocalTime now = nowIST.toLocalTime();

        for (DeliveryHours deliveryHour : deliveryHours) {
            LocalTime from = LocalTime.parse(deliveryHour.getFrom());
            LocalTime to = LocalTime.parse(deliveryHour.getTo());

            if (from.equals(to)) {
                return true;
            }

            if (!now.isBefore(from) && !now.isAfter(to)) {
                return true;
            }
        }
        return false;
    }
}
