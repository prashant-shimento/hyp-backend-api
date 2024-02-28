package com.hyp.util;
import java.time.LocalTime;

public class ValidationUtils {

	public static LocalTime validateLocalTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return LocalTime.MIDNIGHT;
        }
        return LocalTime.parse(timeString);
    }
}
