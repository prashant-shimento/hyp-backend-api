package com.hyp.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Application metrics service for tracking operational metrics.
 * Uses caching to avoid repeated meter registration overhead.
 */
@Component
@Slf4j
public class ApplicationMetrics {

    private final MeterRegistry registry;

    // Cache for meters to avoid re-registration overhead
    private final ConcurrentHashMap<String, Counter> counterCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

    public ApplicationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Increment a counter by 1.
     *
     * @param event The metric event
     * @param tags  Key-value pairs (must be even number). Accepts String or MetricTag.
     */
    public void count(MetricsEvent event, Object... tags) {
        String[] stringTags = toStringTags(tags);
        String key = buildCacheKey(metric(event), stringTags);
        counterCache
                .computeIfAbsent(
                        key,
                        k -> Counter.builder(metric(event)).tags(stringTags).register(registry))
                .increment();
    }

    /**
     * Record a timing measurement.
     *
     * @param event      The metric event
     * @param durationMs Duration in milliseconds
     * @param tags       Key-value pairs (must be even number). Accepts String or MetricTag.
     */
    public void time(MetricsEvent event, long durationMs, Object... tags) {
        String[] stringTags = toStringTags(tags);
        String metricName = metric(event);
        String key = buildCacheKey(metricName, stringTags);
        timerCache
                .computeIfAbsent(key, k -> Timer.builder(metricName)
                        .tags(stringTags)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(registry))
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Get or create an AtomicLong-backed gauge.
     * The same gauge instance is returned for the same name+tags combination.
     *
     * @param name Metric name
     * @param tags Key-value pairs (must be even number). Accepts String or MetricTag.
     * @return AtomicLong that backs the gauge
     */
    public AtomicLong gauge(String name, Object... tags) {
        String[] stringTags = toStringTags(tags);
        String sanitizedName = sanitizeName(name);
        String key = buildCacheKey(sanitizedName, stringTags);
        return gaugeValues.computeIfAbsent(key, k -> {
            AtomicLong value = new AtomicLong(0);
            Gauge.builder(sanitizedName, value, AtomicLong::get)
                    .tags(stringTags)
                    .register(registry);
            return value;
        });
    }

    /**
     * Increment a gauge value by 1.
     *
     * @param name Metric name
     * @param tags Key-value pairs. Accepts String or MetricTag.
     */
    public void incrementGauge(String name, Object... tags) {
        gauge(name, tags).incrementAndGet();
    }

    /**
     * Decrement a gauge value by 1.
     *
     * @param name Metric name
     * @param tags Key-value pairs. Accepts String or MetricTag.
     */
    public void decrementGauge(String name, Object... tags) {
        gauge(name, tags).decrementAndGet();
    }

    /**
     * Set a gauge to a specific value.
     *
     * @param name  Metric name
     * @param value Value to set
     * @param tags  Key-value pairs. Accepts String or MetricTag.
     */
    public void setGauge(String name, long value, Object... tags) {
        gauge(name, tags).set(value);
    }

    /**
     * Start a timer and return a sample for later stopping.
     *
     * @return Timer.Sample to stop later
     */
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    /**
     * Stop a timer and record to the specified metric.
     *
     * @param sample Timer sample from startTimer()
     * @param event  The metric event
     * @param tags   Key-value pairs (must be even number). Accepts String or MetricTag.
     */
    public void stopTimer(Timer.Sample sample, MetricsEvent event, Object... tags) {
        String[] stringTags = toStringTags(tags);
        String metricName = metric(event);
        String key = buildCacheKey(metricName, stringTags);
        Timer timer = timerCache.computeIfAbsent(key, k -> Timer.builder(metricName)
                .tags(stringTags)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry));
        sample.stop(timer);
    }

    // ==================== HELPER METHODS ====================

    // Max length for tag values to prevent cardinality explosion
    private static final int MAX_TAG_VALUE_LENGTH = 100;

    private String metric(MetricsEvent e) {
        return sanitizeName(e.name());
    }

    private String sanitizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    /**
     * Sanitize tag value to prevent cardinality explosion and invalid characters.
     */
    private String sanitizeTagValue(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9_.\\-]", "_");
        if (sanitized.length() > MAX_TAG_VALUE_LENGTH) {
            return sanitized.substring(0, MAX_TAG_VALUE_LENGTH);
        }
        return sanitized;
    }

    /**
     * Convert Object varargs to String array, handling MetricTag enum.
     * Validates and sanitizes all tag values.
     */
    private String[] toStringTags(Object... tags) {
        if (tags.length % 2 != 0) {
            log.warn("Tags must be key-value pairs, got {} tags: {}", tags.length, Arrays.toString(tags));
            throw new IllegalArgumentException("Tags must be key-value pairs, got odd number: " + tags.length);
        }
        String[] result = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            String value;
            if (tags[i] instanceof MetricTag) {
                value = ((MetricTag) tags[i]).key();
            } else if (tags[i] != null) {
                value = tags[i].toString();
            } else {
                value = "unknown";
            }
            // Sanitize tag keys (even indices) and values (odd indices)
            result[i] = sanitizeTagValue(value);
        }
        return result;
    }

    private String buildCacheKey(String metricName, String... tags) {
        if (tags.length == 0) {
            return metricName;
        }
        StringBuilder sb = new StringBuilder(metricName);
        for (int i = 0; i < tags.length; i += 2) {
            sb.append("|").append(tags[i]).append("=").append(tags[i + 1]);
        }
        return sb.toString();
    }
}
