package com.hyp.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rate-limit.login.capacity:10}")
    private int loginCapacity;

    @Value("${rate-limit.login.refill-tokens:10}")
    private int loginRefillTokens;

    @Value("${rate-limit.login.refill-duration:60}")
    private int loginRefillDuration; // seconds

    @Value("${rate-limit.api.capacity:100}")
    private int apiCapacity;

    @Value("${rate-limit.api.refill-tokens:100}")
    private int apiRefillTokens;

    @Value("${rate-limit.api.refill-duration:60}")
    private int apiRefillDuration; // seconds

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String clientKey = resolveClientKey(request);

        // Different rate limits for different endpoints
        Bucket bucket = resolveBucket(clientKey, path);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for client: {} path: {}", clientKey, path);
            sendRateLimitExceededResponse(response, probe.getNanosToWaitForRefill());
        }
    }

    private String resolveClientKey(HttpServletRequest request) {
        // Try to get user ID from authenticated principal
        if (request.getUserPrincipal() != null) {
            return "user:" + request.getUserPrincipal().getName();
        }

        // Fall back to IP address
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // Get first IP if multiple (forwarded chain)
            ip = ip.split(",")[0].trim();
        }

        return "ip:" + ip;
    }

    private Bucket resolveBucket(String clientKey, String path) {
        String bucketKey = clientKey + ":" + getBucketType(path);

        return buckets.computeIfAbsent(bucketKey, key -> {
            if (isLoginEndpoint(path)) {
                return createLoginBucket();
            } else {
                return createApiBucket();
            }
        });
    }

    private String getBucketType(String path) {
        if (isLoginEndpoint(path)) {
            return "login";
        }
        return "api";
    }

    private boolean isLoginEndpoint(String path) {
        return path.contains("/login/") || path.contains("/auth/");
    }

    private Bucket createLoginBucket() {
        // Stricter rate limit for login endpoints (prevent brute force)
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(loginCapacity)
                        .refillGreedy(loginRefillTokens, Duration.ofSeconds(loginRefillDuration))
                        .build())
                .build();
    }

    private Bucket createApiBucket() {
        // General API rate limit
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(apiCapacity)
                        .refillGreedy(apiRefillTokens, Duration.ofSeconds(apiRefillDuration))
                        .build())
                .build();
    }

    private void sendRateLimitExceededResponse(HttpServletResponse response, long nanosToWait) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(429); // Too Many Requests
        response.addHeader("Retry-After", String.valueOf(nanosToWait / 1_000_000_000));

        Map<String, Object> body = new HashMap<>();
        body.put("error", true);
        body.put("message", "Rate limit exceeded. Please try again later.");
        body.put("errorCode", "RATE_LIMIT_EXCEEDED");
        body.put("retryAfterSeconds", nanosToWait / 1_000_000_000);

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for health checks, static resources, and WebSocket endpoints
        // (SockJS makes many rapid HTTP requests that would exhaust the rate limit bucket)
        return path.startsWith("/actuator/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.contains("/ws");
    }

    /**
     * Clear all rate limit buckets (for testing or admin use)
     */
    public void clearAllBuckets() {
        buckets.clear();
        log.info("All rate limit buckets cleared");
    }

    /**
     * Clear rate limit for a specific client
     */
    public void clearBucketForClient(String clientKey) {
        buckets.entrySet().removeIf(entry -> entry.getKey().startsWith(clientKey));
        log.info("Rate limit cleared for client: {}", clientKey);
    }
}
