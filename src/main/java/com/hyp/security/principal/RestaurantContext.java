package com.hyp.security.principal;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local holder for the current restaurant context.
 * Used for multi-tenancy - automatically filters queries by restaurantId.
 */
@Slf4j
public class RestaurantContext {

    private static final ThreadLocal<String> restaurantIdHolder = new ThreadLocal<>();

    private RestaurantContext() {
        // Utility class - prevent instantiation
    }

    public static void setRestaurantId(String restaurantId) {
        log.debug("Setting restaurant context: {}", restaurantId);
        restaurantIdHolder.set(restaurantId);
    }

    public static String getRestaurantId() {
        return restaurantIdHolder.get();
    }

    public static void clear() {
        log.debug("Clearing restaurant context");
        restaurantIdHolder.remove();
    }

    public static boolean hasContext() {
        return restaurantIdHolder.get() != null;
    }
}
