package com.hyp.observability;

import java.util.concurrent.Callable;
import org.slf4j.MDC;

/**
 * Utility for setting order-flow MDC context programmatically.
 *
 * Use this in:
 * - Temporal workflow activities
 * - Async operations
 * - Anywhere order context is known but not in HTTP request scope
 *
 * MDC fields managed:
 * - order_id: The order being processed
 * - restaurant_id: The restaurant for the order
 *
 * Note: request_id is only set by MdcLoggingFilter for HTTP requests.
 * Non-HTTP contexts (Temporal, async) won't have request_id.
 */
public final class ObservabilityContext {

    public static final String ORDER_ID = "order_id";
    public static final String RESTAURANT_ID = "restaurant_id";

    private ObservabilityContext() {}

    /**
     * Set order context for logging.
     */
    public static void setOrderId(String orderId) {
        if (orderId != null && !orderId.isBlank()) {
            MDC.put(ORDER_ID, orderId);
        }
    }

    /**
     * Set restaurant context for logging.
     */
    public static void setRestaurantId(String restaurantId) {
        if (restaurantId != null && !restaurantId.isBlank()) {
            MDC.put(RESTAURANT_ID, restaurantId);
        }
    }

    /**
     * Set both order and restaurant context.
     */
    public static void setOrderContext(String orderId, String restaurantId) {
        setOrderId(orderId);
        setRestaurantId(restaurantId);
    }

    /**
     * Get current order ID from MDC.
     */
    public static String getOrderId() {
        return MDC.get(ORDER_ID);
    }

    /**
     * Get current restaurant ID from MDC.
     */
    public static String getRestaurantId() {
        return MDC.get(RESTAURANT_ID);
    }

    /**
     * Clear order-flow MDC context.
     * Call this in finally blocks when context was set programmatically.
     */
    public static void clear() {
        MDC.remove(ORDER_ID);
        MDC.remove(RESTAURANT_ID);
    }

    /**
     * Execute a runnable with order context, clearing after execution.
     *
     * Example:
     * <pre>
     * ObservabilityContext.withOrderContext(orderId, restaurantId, () -> {
     *     log.info("Processing order");
     *     // ... order processing logic
     * });
     * </pre>
     */
    public static void withOrderContext(String orderId, String restaurantId, Runnable runnable) {
        try {
            setOrderContext(orderId, restaurantId);
            runnable.run();
        } finally {
            clear();
        }
    }

    /**
     * Execute a callable with order context, clearing after execution.
     *
     * Example:
     * <pre>
     * Order result = ObservabilityContext.withOrderContext(orderId, restaurantId, () -> {
     *     log.info("Processing order");
     *     return orderService.process(orderId);
     * });
     * </pre>
     */
    public static <T> T withOrderContext(String orderId, String restaurantId, Callable<T> callable) throws Exception {
        try {
            setOrderContext(orderId, restaurantId);
            return callable.call();
        } finally {
            clear();
        }
    }
}
