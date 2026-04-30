package com.hyp.security.policy;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * Utility for matching request paths against policy patterns.
 * Supports:
 * - Exact matches: /customer
 * - Wildcard segments: /order/{id}
 * - Multi-segment wildcards: /menu/**
 */
@Component
public class PathMatcher {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * Check if a request path matches a policy pattern
     */
    public boolean matches(String pattern, String path) {
        // Normalize paths
        String normalizedPattern = normalizePath(pattern);
        String normalizedPath = normalizePath(path);

        // Convert {id} style placeholders to * for AntPathMatcher
        String antPattern = convertToAntPattern(normalizedPattern);

        return antPathMatcher.match(antPattern, normalizedPath);
    }

    /**
     * Check if a request method matches a policy operation
     */
    public boolean matchesOperation(String policyOperation, String requestMethod) {
        if ("*".equals(policyOperation)) {
            return true;
        }
        return policyOperation.equalsIgnoreCase(requestMethod);
    }

    /**
     * Normalize a path by removing leading/trailing slashes and api version prefix
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        // Remove api version prefix if present
        String normalized = path;
        if (normalized.startsWith("/api/v2/")) {
            normalized = normalized.substring(7); // Remove "/api/v2"
        } else if (normalized.startsWith("/api/v3/")) {
            normalized = normalized.substring(7); // Remove "/api/v3"
        }

        // Ensure path starts with /
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        // Remove trailing slash if present (except for root)
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * Convert {placeholder} style patterns to * for AntPathMatcher
     */
    private String convertToAntPattern(String pattern) {
        // Replace {anything} with *
        return Pattern.compile("\\{[^}]+\\}").matcher(pattern).replaceAll("*");
    }

    /**
     * Get the specificity score of a pattern (more specific = higher score)
     * Used for sorting patterns when multiple match
     */
    public int getPatternSpecificity(String pattern) {
        int score = 0;

        // Exact paths are most specific
        if (!pattern.contains("*") && !pattern.contains("{")) {
            score += 1000;
        }

        // Count path segments
        String[] segments = pattern.split("/");
        score += segments.length * 10;

        // Wildcards reduce specificity
        if (pattern.contains("**")) {
            score -= 500;
        }
        if (pattern.contains("*") && !pattern.contains("**")) {
            score -= 50;
        }
        if (pattern.contains("{")) {
            score -= 25;
        }

        return score;
    }
}
