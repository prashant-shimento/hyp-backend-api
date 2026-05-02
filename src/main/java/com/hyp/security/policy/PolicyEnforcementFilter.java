package com.hyp.security.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.security.exception.AuthorizationException;
import com.hyp.security.exception.JwtAuthenticationException;
import com.hyp.security.jwt.JwtAuthenticationFilter;
import com.hyp.security.principal.RestaurantContext;
import com.hyp.security.principal.UserPrincipal;
import com.hyp.validation.RestaurantValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolicyEnforcementFilter extends OncePerRequestFilter {

    private final AccessPolicyService accessPolicyService;
    private final RestaurantValidator restaurantValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String REQUIRE_OWNER_CHECK_ATTRIBUTE = "requireOwnerCheck";
    public static final String RESTAURANT_SCOPED_ATTRIBUTE = "restaurantScoped";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug("PolicyEnforcementFilter: {} {}", method, path);

        try {
            // Get current principal (may be null for unauthenticated requests)
            UserPrincipal principal = getCurrentPrincipal();

            // Validate restaurant ID if provided in header/context
            String restaurantId = RestaurantContext.getRestaurantId();
            if (restaurantId != null && !restaurantId.isBlank()) {
                // Validate restaurant exists
                if (!restaurantValidator.restaurantExists(restaurantId)) {
                    log.warn("Invalid restaurant ID in header: {}", restaurantId);
                    sendErrorResponse(
                            response,
                            HttpServletResponse.SC_BAD_REQUEST,
                            "The provided restaurant ID is not valid.",
                            "INVALID_RESTAURANT_ID");
                    return;
                }

                // Validate user has access to this restaurant (if authenticated and not super admin)
                if (principal != null && !principal.isSuperAdmin() && !principal.canAccessRestaurant(restaurantId)) {
                    log.warn(
                            "User {} attempted to access unauthorized restaurant: {}",
                            principal.getUserId(),
                            restaurantId);
                    sendErrorResponse(
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            "You are not authorized to access this restaurant's data.",
                            "RESTAURANT_ACCESS_DENIED");
                    return;
                }
            }

            // Evaluate access policy
            PolicyDecision decision = accessPolicyService.evaluate(path, method, principal);

            switch (decision.getResult()) {
                case ALLOW -> {
                    // For restaurant-scoped policies, ensure restaurant ID is provided
                    if (decision.isRestaurantScoped()) {
                        if (restaurantId == null || restaurantId.isBlank()) {
                            sendErrorResponse(
                                    response,
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    "X-Restaurant-Id header is required",
                                    "RESTAURANT_ID_REQUIRED");
                            return;
                        }
                        // Access validation already done above
                        request.setAttribute(RESTAURANT_SCOPED_ATTRIBUTE, true);
                    }
                    filterChain.doFilter(request, response);
                }
                case REQUIRE_OWNER_CHECK -> {
                    // Allow request but mark for owner check in service layer
                    request.setAttribute(REQUIRE_OWNER_CHECK_ATTRIBUTE, true);
                    if (decision.isRestaurantScoped()) {
                        if (restaurantId == null || restaurantId.isBlank()) {
                            sendErrorResponse(
                                    response,
                                    HttpServletResponse.SC_BAD_REQUEST,
                                    "X-Restaurant-Id header is required",
                                    "RESTAURANT_ID_REQUIRED");
                            return;
                        }
                        request.setAttribute(RESTAURANT_SCOPED_ATTRIBUTE, true);
                    }
                    filterChain.doFilter(request, response);
                }
                case REQUIRE_AUTH -> {
                    String jwtError = (String) request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);
                    if ("TOKEN_EXPIRED".equals(jwtError)) {
                        sendErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Your session has expired. Please log in again.",
                                "TOKEN_EXPIRED");
                    } else if ("INVALID_TOKEN".equals(jwtError)) {
                        sendErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Invalid authentication token.",
                                "INVALID_TOKEN");
                    } else if ("WRONG_TOKEN_TYPE".equals(jwtError)) {
                        sendErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Please use an access token, not a refresh token.",
                                "WRONG_TOKEN_TYPE");
                    } else {
                        sendErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Authentication required.",
                                "UNAUTHORIZED");
                    }
                }
                case DENY -> {
                    sendErrorResponse(
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            decision.getReason() != null ? decision.getReason() : "Access denied",
                            "FORBIDDEN");
                }
            }
        } catch (JwtAuthenticationException e) {
            log.error("Authentication error in PolicyEnforcementFilter: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage(), "UNAUTHORIZED");
        } catch (AuthorizationException e) {
            log.error("Authorization error in PolicyEnforcementFilter: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage(), e.getErrorCode());
        } catch (Exception e) {
            log.error("Error in PolicyEnforcementFilter", e);
            sendErrorResponse(
                    response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error", "INTERNAL_ERROR");
        }
    }

    private UserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                return (UserPrincipal) principal;
            }
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message, String errorCode)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status);

        Map<String, Object> body = new HashMap<>();
        body.put("error", true);
        body.put("message", message);
        body.put("errorCode", errorCode);

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip filtering for v2 endpoints (legacy, backward compatible)
        if (path.startsWith("/api/v2/")) {
            return true;
        }

        // Skip filtering for static resources and health checks
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/actuator/health")
                || path.equals("/favicon.ico");
    }
}
