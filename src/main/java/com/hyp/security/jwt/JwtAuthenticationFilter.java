package com.hyp.security.jwt;

import com.hyp.entity.Customer;
import com.hyp.entity.User;
import com.hyp.security.principal.RestaurantContext;
import com.hyp.security.principal.UserPrincipal;
import com.hyp.security.service.RoleService;
import com.hyp.service.CustomerService;
import com.hyp.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RoleService roleService;
    private final CustomerService customerService;
    private final UserService userService;

    public static final String JWT_ERROR_ATTRIBUTE = "jwt.error";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String RESTAURANT_ID_HEADER = "X-Restaurant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // Single parse: validate + type-check + principal extraction in one HMAC operation
                UserPrincipal principal = jwtTokenProvider.validateAndExtractAccessPrincipal(jwt);
                if (principal == null) {
                    // Token present but rejected — store reason so PolicyEnforcementFilter
                    // can return a specific message instead of the generic "Authentication required".
                    request.setAttribute(JWT_ERROR_ATTRIBUTE, jwtTokenProvider.getJwtFailureReason(jwt));
                } else {
                    // Authenticate immediately — JWT validity is the proof of authentication.
                    // Enrichment failures (cache miss, transient DB issue) must not revoke auth.
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Enrich: derive isSuperAdmin from the role cache (L1 Caffeine), not from the
                    // token claim. This ensures role revocations take effect within one cache TTL
                    // (5 min) rather than waiting for the token to expire (30 min).
                    try {
                        principal.setSuperAdmin(roleService.isSuperAdmin(principal.getRole()));
                    } catch (Exception e) {
                        log.warn(
                                "Failed to derive isSuperAdmin for user {}: {}", principal.getUserId(), e.getMessage());
                    }

                    // Enrich: load restaurant access list from cache (L1 Caffeine → L2 Redis → DB).
                    // Not embedded in the JWT — a user with many restaurants would bloat every
                    // request header. The cache lookup is <1ms on a warm L1.
                    try {
                        if ("CUSTOMER".equals(principal.getUserType())) {
                            Customer customer = customerService.findById(principal.getUserId());
                            if (customer != null) {
                                principal.setRestaurants(customer.getRestaurants());
                            }
                        } else if ("USER".equals(principal.getUserType())) {
                            User user = userService.findById(principal.getUserId());
                            if (user != null) {
                                principal.setRestaurants(user.getRestaurantIds());
                            }
                        }
                    } catch (Exception e) {
                        log.warn(
                                "Failed to load restaurant access for user {}: {}",
                                principal.getUserId(),
                                e.getMessage());
                    }

                    log.debug("Authenticated user: {} with role: {}", principal.getUserId(), principal.getRole());
                }
            }

            // Extract restaurant context from header or parameter
            extractRestaurantContext(request);

        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
            // Clear context on error
            SecurityContextHolder.clearContext();
            RestaurantContext.clear();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clear the restaurant context after request processing
            RestaurantContext.clear();
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void extractRestaurantContext(HttpServletRequest request) {
        // Get restaurant ID from header or query param - REQUIRED for restaurant-scoped operations
        String headerRestaurantId = request.getHeader(RESTAURANT_ID_HEADER);
        if (!StringUtils.hasText(headerRestaurantId)) {
            headerRestaurantId = request.getParameter("restaurantId");
        }

        // Only set context if explicitly provided via header/param
        // PolicyEnforcementFilter will reject restaurant-scoped requests without context
        if (StringUtils.hasText(headerRestaurantId)) {
            RestaurantContext.setRestaurantId(headerRestaurantId);
        }
        // No fallback to token's default - forces explicit header for restaurant-scoped operations
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filtering for static resources
        // Skip WebSocket paths — browsers can't send Authorization headers on WS upgrades.
        // Auth is enforced at the STOMP CONNECT frame level instead (see WebSocketSecurityConfig).
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health")
                || path.contains("/ws");
    }
}
