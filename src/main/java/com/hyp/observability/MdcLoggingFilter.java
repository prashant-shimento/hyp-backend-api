package com.hyp.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * MDC filter for request-level observability.
 *
 * Design principles:
 * - request_id: Always populated for all requests (correlation)
 * - order_id, restaurant_id: Only populated for order-related APIs
 * - No body parsing (too aggressive, context should be set programmatically)
 * - No distributed tracing (monolithic architecture)
 *
 * For non-HTTP contexts (Temporal workflows, async tasks), use
 * {@link ObservabilityContext} to set MDC programmatically.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    // MDC keys
    public static final String REQUEST_ID = "request_id";
    public static final String ORDER_ID = "order_id";
    public static final String RESTAURANT_ID = "restaurant_id";

    // Order-related path prefixes (case-insensitive matching)
    private static final Set<String> ORDER_FLOW_PATHS =
            Set.of("/order", "/orders", "/pos", "/delivery", "/payment", "/checkout");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Always set request_id for all requests
            MDC.put(REQUEST_ID, generateRequestId());

            // Only extract order context for order-related APIs
            String path = request.getRequestURI();
            if (isOrderFlowPath(path)) {
                extractOrderContextFromPath(path);
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Check if the request path is related to order flow.
     */
    private boolean isOrderFlowPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        String lowerPath = path.toLowerCase();
        for (String prefix : ORDER_FLOW_PATHS) {
            if (lowerPath.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract order_id and restaurant_id from URL path segments.
     * Pattern: /orders/{orderId} or /restaurants/{restaurantId}/orders/...
     */
    private void extractOrderContextFromPath(String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        String[] segments = path.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            String key = segments[i].toLowerCase();
            String value = segments[i + 1];

            if (!isValidId(value)) {
                continue;
            }

            switch (key) {
                case "order", "orders" -> putIfAbsent(ORDER_ID, value);
                case "restaurant", "restaurants" -> putIfAbsent(RESTAURANT_ID, value);
                default -> {
                    // Ignore other path segments
                }
            }
        }
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void putIfAbsent(String key, String value) {
        if (MDC.get(key) == null) {
            MDC.put(key, value);
        }
    }

    private boolean isValidId(String value) {
        return value != null && !value.isBlank() && value.length() >= 3 && value.length() <= 50;
    }
}
