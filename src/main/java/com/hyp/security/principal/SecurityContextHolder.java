package com.hyp.security.principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

/**
 * Utility class to access the current authenticated principal.
 */
public class SecurityContextHolder {

    private SecurityContextHolder() {
        // Utility class - prevent instantiation
    }

    public static UserPrincipal getPrincipal() {
        SecurityContext context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }

        return null;
    }

    public static String getCurrentUserId() {
        UserPrincipal principal = getPrincipal();
        return principal != null ? principal.getUserId() : null;
    }

    public static String getCurrentRole() {
        UserPrincipal principal = getPrincipal();
        return principal != null ? principal.getRole() : null;
    }

    public static boolean isSuperAdmin() {
        UserPrincipal principal = getPrincipal();
        return principal != null && principal.isSuperAdmin();
    }

    public static boolean isAuthenticated() {
        return getPrincipal() != null;
    }
}
