package com.hyp.observability;

/**
 * Standardized metric tag keys for consistent metric labeling.
 * Use these constants to ensure tag key consistency across all metrics.
 */
public enum MetricTag {
    ACTION("action"),
    STATUS("status"),
    REASON("reason"),
    RESULT("result"),
    TYPE("type"),
    SERVICE("service"),
    CHANNEL("channel"),
    PARTNER("partner"),
    ORDER_TYPE("order_type");

    private final String key;

    MetricTag(String key) {
        this.key = key;
    }

    /**
     * Get the string key for this tag.
     *
     * @return The tag key string
     */
    public String key() {
        return key;
    }

    /**
     * Alias for key() to maintain compatibility with String constant usage.
     */
    @Override
    public String toString() {
        return key;
    }
}
