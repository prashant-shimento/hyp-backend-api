package com.hyp.security.audit;

import com.hyp.security.principal.RestaurantContext;
import com.hyp.security.principal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(request, response, duration);
        }
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();
        String restaurantId = RestaurantContext.getRestaurantId();

        UserPrincipal principal = getCurrentPrincipal();
        if (principal != null) {
            log.info(
                    "[AUDIT] userId={} userType={} role={} restaurantId={} method={} path={} status={} duration={}ms",
                    principal.getUserId(),
                    principal.getUserType(),
                    principal.getRole(),
                    restaurantId != null ? restaurantId : "-",
                    method,
                    path,
                    status,
                    duration);
        } else {
            log.debug("[AUDIT] anonymous method={} path={} status={} duration={}ms", method, path, status, duration);
        }
    }

    private UserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal userPrincipal) {
                return userPrincipal;
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health")
                || path.equals("/favicon.ico");
    }
}
