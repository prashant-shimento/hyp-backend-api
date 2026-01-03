package com.hyp.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Production-safe aspect for automatic method timing and metrics collection.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Low cardinality: Avoids high-cardinality tags (method names) in automatic aspects</li>
 *   <li>Status-based: Uses status=success|error tag instead of separate _errors metrics</li>
 *   <li>No double-counting: @Timed methods are excluded from service aspect</li>
 *   <li>Cached meters: Avoids repeated registration overhead</li>
 *   <li>Configurable: Enable/disable via property</li>
 * </ul>
 *
 * <p>Enable/disable via property: metrics.aspect.enabled=true/false (default: true)</p>
 */
@Aspect
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "metrics.aspect.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsAspect {

    private final MeterRegistry meterRegistry;

    // Cache for timers to avoid repeated registration overhead
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();

    // Allow-list for repository tracking to control cardinality
    private static final Set<String> TRACKED_REPOSITORIES = Set.of(
            "OrderRepository",
            "MenuRepository",
            "CategoryRepository",
            "ItemRepository",
            "SettlementRepository",
            "DeliveryRepository",
            "PaymentRepository",
            "RestaurantRepository",
            "CustomerRepository");

    // Allow-list for tracked services (null = track all)
    private static final Set<String> TRACKED_SERVICES = Set.of(
            "OrderService",
            "DeliveryService",
            "PaymentService",
            "SettlementService",
            "NotificationService",
            "RestaurantService",
            "PosService");

    // Thresholds for slow operation logging (logs only, not metrics)
    private static final long SLOW_METHOD_THRESHOLD_MS = 1000;
    private static final long SLOW_QUERY_THRESHOLD_MS = 500;

    // Max length for tag values to prevent cardinality explosion
    private static final int MAX_TAG_VALUE_LENGTH = 50;

    public MetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Times methods annotated with @Timed.
     * This is user-controlled and allows method-level granularity.
     */
    @Around("@annotation(timed)")
    public Object timeAnnotatedMethod(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = sanitizeTagValue(signature.getDeclaringType().getSimpleName());
        String methodName = sanitizeTagValue(signature.getName());

        String metricName = timed.value().isEmpty() ? "app_method" : sanitizeMetricName(timed.value());

        long startTime = System.nanoTime();
        String status = "success";
        String exceptionClass = "";

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            status = "error";
            exceptionClass = sanitizeTagValue(e.getClass().getSimpleName());
            throw e;
        } finally {
            long durationNanos = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

            // Build timer with status tag (not separate error counter)
            String timerKey = buildCacheKey(metricName, className, methodName, status, timed.extraTags());
            Timer timer = getOrCreateTimer(timerKey, metricName, timed, className, methodName, status, exceptionClass);
            timer.record(durationNanos, TimeUnit.NANOSECONDS);

            // Log slow methods (logs only, not metrics)
            if (durationMs > SLOW_METHOD_THRESHOLD_MS) {
                log.warn("Slow method: {}.{} took {}ms status={}", className, methodName, durationMs, status);
            }
        }
    }

    /**
     * Times service methods with LOW CARDINALITY (service name only, no method name).
     * Excludes methods annotated with @Timed to prevent double-counting.
     */
    @Around("execution(* com.hyp.service.*Service.*(..)) && !@annotation(com.hyp.observability.Timed)")
    public Object timeServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String serviceName = signature.getDeclaringType().getSimpleName();

        // Allow-list check for cardinality control
        if (!TRACKED_SERVICES.contains(serviceName)) {
            return joinPoint.proceed();
        }

        String methodName = signature.getName(); // For logging only, not metrics

        long startTime = System.nanoTime();
        String status = "success";

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            status = "error";
            throw e;
        } finally {
            long durationNanos = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

            // LOW CARDINALITY: Only service name + status, NO method name
            final String finalStatus = status;
            String timerKey = "service|" + serviceName + "|" + finalStatus;
            Timer timer = timerCache.computeIfAbsent(timerKey, k -> Timer.builder("hyp_service")
                    .tag("service", serviceName)
                    .tag("status", finalStatus)
                    .description("Service method execution time")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));

            timer.record(durationNanos, TimeUnit.NANOSECONDS);

            // Log slow methods (logs capture method name for debugging)
            if (durationMs > SLOW_METHOD_THRESHOLD_MS) {
                log.warn("Slow service: {}.{} took {}ms status={}", serviceName, methodName, durationMs, finalStatus);
            }
        }
    }

    /**
     * Times controller methods. Controllers have bounded cardinality (finite endpoints).
     * Excludes methods annotated with @Timed to prevent double-counting.
     */
    @Around("execution(* com.hyp.controller.*Controller.*(..)) && !@annotation(com.hyp.observability.Timed)")
    public Object timeControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String controllerName = sanitizeTagValue(signature.getDeclaringType().getSimpleName());
        String methodName = sanitizeTagValue(signature.getName());

        long startTime = System.nanoTime();
        String status = "success";

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            status = "error";
            throw e;
        } finally {
            long durationNanos = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

            // Controllers have bounded cardinality, so method name is acceptable
            final String finalStatus = status;
            String timerKey = "controller|" + controllerName + "|" + methodName + "|" + finalStatus;
            Timer timer = timerCache.computeIfAbsent(timerKey, k -> Timer.builder("hyp_controller")
                    .tag("controller", controllerName)
                    .tag("endpoint", methodName)
                    .tag("status", finalStatus)
                    .description("Controller endpoint execution time")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));

            timer.record(durationNanos, TimeUnit.NANOSECONDS);

            if (durationMs > SLOW_METHOD_THRESHOLD_MS) {
                log.warn(
                        "Slow controller: {}.{} took {}ms status={}",
                        controllerName,
                        methodName,
                        durationMs,
                        finalStatus);
            }
        }
    }

    /**
     * Times repository operations with LOW CARDINALITY (repository name only).
     * Only tracks allow-listed repositories.
     */
    @Around("execution(* com.hyp.repository.*Repository.*(..))")
    public Object timeRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String repoName = signature.getDeclaringType().getSimpleName();

        // Allow-list check
        if (!TRACKED_REPOSITORIES.contains(repoName)) {
            return joinPoint.proceed();
        }

        String methodName = signature.getName(); // For logging only

        long startTime = System.nanoTime();
        String status = "success";

        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            status = "error";
            throw e;
        } finally {
            long durationNanos = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

            // LOW CARDINALITY: Only repository name + status, NO method name
            final String finalStatus = status;
            String timerKey = "repository|" + repoName + "|" + finalStatus;
            Timer timer = timerCache.computeIfAbsent(timerKey, k -> Timer.builder("hyp_repository")
                    .tag("repository", repoName)
                    .tag("status", finalStatus)
                    .description("Repository operation time")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));

            timer.record(durationNanos, TimeUnit.NANOSECONDS);

            // Log slow queries (logs capture method name for debugging)
            if (durationMs > SLOW_QUERY_THRESHOLD_MS) {
                log.warn("Slow query: {}.{} took {}ms status={}", repoName, methodName, durationMs, finalStatus);
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private Timer getOrCreateTimer(
            String timerKey,
            String metricName,
            Timed timed,
            String className,
            String methodName,
            String status,
            String exceptionClass) {

        return timerCache.computeIfAbsent(timerKey, k -> {
            Timer.Builder builder = Timer.builder(metricName)
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", status);

            // Add exception tag only on error (limited to class name)
            if ("error".equals(status) && !exceptionClass.isEmpty()) {
                builder.tag("exception", exceptionClass);
            }

            if (!timed.description().isEmpty()) {
                builder.description(timed.description());
            }

            if (timed.percentiles()) {
                builder.publishPercentiles(0.5, 0.75, 0.95, 0.99);
            }

            // Add extra tags from annotation
            String[] extraTags = timed.extraTags();
            for (int i = 0; i < extraTags.length - 1; i += 2) {
                builder.tag(sanitizeTagValue(extraTags[i]), sanitizeTagValue(extraTags[i + 1]));
            }

            return builder.register(meterRegistry);
        });
    }

    private String buildCacheKey(
            String metricName, String className, String methodName, String status, String[] extraTags) {
        StringBuilder sb = new StringBuilder("timed|")
                .append(metricName)
                .append("|")
                .append(className)
                .append("|")
                .append(methodName)
                .append("|")
                .append(status);

        for (int i = 0; i < extraTags.length - 1; i += 2) {
            sb.append("|").append(extraTags[i]).append("=").append(extraTags[i + 1]);
        }
        return sb.toString();
    }

    private String sanitizeMetricName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    private String sanitizeTagValue(String value) {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9_.-]", "_");
        if (sanitized.length() > MAX_TAG_VALUE_LENGTH) {
            return sanitized.substring(0, MAX_TAG_VALUE_LENGTH);
        }
        return sanitized;
    }
}
