package com.hyp.security.apikey;

import com.hyp.entity.IntegrationKey;
import com.hyp.repository.IntegrationKeyRepository;
import com.hyp.security.principal.UserPrincipal;
import com.hyp.security.service.SecurityCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final IntegrationKeyRepository integrationKeyRepository;
    private final HmacSignatureValidator hmacSignatureValidator;
    private final SecurityCacheService securityCacheService;

    // Debounce lastUsedAt writes: at most once per key per interval
    private static final long LAST_USED_DEBOUNCE_SECONDS = 60;
    private final ConcurrentHashMap<String, Instant> lastWrittenAt = new ConcurrentHashMap<>();

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only process if API key is present
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Processing API key authentication for path: {}", request.getRequestURI());

        try {
            // Look up integration key from L1/L2 cache (DB fallback on cold read)
            Optional<IntegrationKey> integrationKeyOpt = securityCacheService.getOrLoadIntegrationKey(apiKey);

            if (integrationKeyOpt.isEmpty()) {
                log.warn("Invalid API key attempted: {}", maskApiKey(apiKey));
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
                return;
            }

            IntegrationKey integrationKey = integrationKeyOpt.get();

            // Validate IP address if IP restriction is enabled
            if (integrationKey.isRequireIpValidation()) {
                String clientIp = getClientIpAddress(request);
                if (!isIpAllowed(clientIp, integrationKey.getAllowedIps())) {
                    log.warn("IP address not allowed for API key: {} - IP: {}", integrationKey.getName(), clientIp);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "IP address not allowed");
                    return;
                }
                log.debug("IP validation passed for: {} from IP: {}", integrationKey.getName(), clientIp);
            }

            // Validate based on auth mode
            IntegrationKey.AuthMode authMode = integrationKey.getAuthMode();
            if (authMode == null) {
                // Default to STATIC_TOKEN for backward compatibility
                authMode = IntegrationKey.AuthMode.STATIC_TOKEN;
            }

            if (authMode == IntegrationKey.AuthMode.HMAC_SIGNATURE) {
                // HMAC signature validation required
                if (!StringUtils.hasText(integrationKey.getSecret())) {
                    log.error("HMAC auth mode but no secret configured for API key: {}", integrationKey.getName());
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error");
                    return;
                }

                // Wrap request to allow reading body multiple times
                ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper
                        ? (ContentCachingRequestWrapper) request
                        : new ContentCachingRequestWrapper(request);

                String signature = request.getHeader(SIGNATURE_HEADER);
                String timestamp = request.getHeader(TIMESTAMP_HEADER);

                if (!StringUtils.hasText(signature)) {
                    log.warn("Missing signature header for API key: {}", integrationKey.getName());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing signature");
                    return;
                }

                // Read request body for signature validation
                String body = StreamUtils.copyToString(wrappedRequest.getInputStream(), StandardCharsets.UTF_8);

                boolean isValid;
                if (StringUtils.hasText(timestamp)) {
                    isValid = hmacSignatureValidator.validateSignatureWithTimestamp(
                            body, signature, timestamp, integrationKey.getSecret());
                } else {
                    isValid = hmacSignatureValidator.validateSignature(body, signature, integrationKey.getSecret());
                }

                if (!isValid) {
                    log.warn("Invalid signature for API key: {}", integrationKey.getName());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                    return;
                }

                log.debug("HMAC signature validation passed for: {}", integrationKey.getName());

                // Use wrapped request for downstream processing
                request = wrappedRequest;
            } else {
                // STATIC_TOKEN mode - API key already validated above
                log.debug("Static token authentication passed for: {}", integrationKey.getName());
            }

            // Create authentication principal
            UserPrincipal principal = UserPrincipal.builder()
                    .userId(integrationKey.getId())
                    .userType("PARTNER")
                    .role(integrationKey.getRole() != null ? integrationKey.getRole() : "PARTNER")
                    .name(integrationKey.getName())
                    .restaurantId(integrationKey.getRestaurantId())
                    .partnerId(integrationKey.getPartnerId())
                    .restaurants(
                            integrationKey.getRestaurantId() != null
                                    ? Collections.singleton(integrationKey.getRestaurantId())
                                    : Collections.emptySet())
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Update last used timestamp (debounced: at most once per key per minute)
            updateLastUsedDebounced(apiKey, integrationKey);

            log.debug("API key authenticated successfully: {}", integrationKey.getName());

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error processing API key authentication", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Process API key authentication for any request that has X-API-Key header
        // This supports:
        // 1. Webhook callbacks (POS, Delivery, Payment)
        // 2. API Partner integrations (RESTAURANT_API_PARTNER, AI_AGENT)
        // Partners use the same /v2/* and /v3/* endpoints with API key auth instead of JWT
        String apiKey = request.getHeader(API_KEY_HEADER);
        return !StringUtils.hasText(apiKey); // Skip filter only if no API key
    }

    /**
     * Write lastUsedAt to DB at most once per LAST_USED_DEBOUNCE_SECONDS per key.
     * Uses a pod-local ConcurrentHashMap to track when each key was last written.
     * The compute() call is atomic so concurrent requests don't produce double writes.
     */
    private void updateLastUsedDebounced(String apiKey, IntegrationKey integrationKey) {
        Instant now = Instant.now();
        boolean[] shouldWrite = {false};
        lastWrittenAt.compute(apiKey, (k, last) -> {
            if (last == null || Duration.between(last, now).getSeconds() >= LAST_USED_DEBOUNCE_SECONDS) {
                shouldWrite[0] = true;
                return now;
            }
            return last;
        });
        if (shouldWrite[0]) {
            integrationKey.setLastUsedAt(LocalDateTime.now());
            integrationKeyRepository.save(integrationKey);
        }
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Get client IP address, handling proxies and load balancers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check common proxy headers in order of preference
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_CLIENT_IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one (original client)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Check if the client IP is in the allowed list
     */
    private boolean isIpAllowed(String clientIp, java.util.Set<String> allowedIps) {
        if (allowedIps == null || allowedIps.isEmpty()) {
            // No IP restriction configured - allow all
            return true;
        }

        // Direct IP match
        if (allowedIps.contains(clientIp)) {
            return true;
        }

        // Check for CIDR notation support (basic implementation)
        for (String allowedIp : allowedIps) {
            if (allowedIp.contains("/")) {
                // CIDR notation - do subnet matching
                if (isIpInCidrRange(clientIp, allowedIp)) {
                    return true;
                }
            } else if (allowedIp.endsWith(".*")) {
                // Wildcard notation (e.g., 192.168.1.*)
                String prefix = allowedIp.substring(0, allowedIp.length() - 1);
                if (clientIp.startsWith(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if an IP is within a CIDR range (basic implementation for IPv4)
     */
    private boolean isIpInCidrRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }

            String networkAddress = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkAddress);

            long mask = -1L << (32 - prefixLength);

            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            log.warn("Error parsing CIDR range: {}", cidr);
            return false;
        }
    }

    /**
     * Convert IPv4 address to long for comparison
     */
    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
        }

        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(octets[i]);
        }
        return result;
    }
}
