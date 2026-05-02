package com.hyp.controller;

import com.hyp.response.Response;
import com.hyp.security.jwt.JwtTokenService;
import com.hyp.security.principal.UserPrincipal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v3/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenService jwtTokenService;

    @PostMapping("/refresh")
    public ResponseEntity<Response> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            Map<String, Object> tokens = jwtTokenService.refreshAccessToken(request.refreshToken());

            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("accessToken", tokens.get("accessToken"));
            responseData.put("tokenType", tokens.get("tokenType"));
            responseData.put("expiresIn", tokens.get("expiresIn"));

            return ResponseEntity.ok(
                    new Response(Collections.singletonList(responseData), false, "Token refreshed successfully"));
        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(null, true, "Invalid or expired refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Response> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            // Revoke specific refresh token if provided
            if (request != null && request.refreshToken() != null) {
                jwtTokenService.revokeRefreshToken(request.refreshToken());
            }

            log.info("User logged out: {} ({})", principal.getUserId(), principal.getUserType());
        }

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new Response(null, false, "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Response> logoutAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            // Revoke all tokens for this user
            jwtTokenService.revokeAllUserTokens(principal.getUserId());
            log.info("All sessions logged out for user: {} ({})", principal.getUserId(), principal.getUserType());
        }

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new Response(null, false, "All sessions logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<Response> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Response(null, true, "Not authenticated"));
        }

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("userId", principal.getUserId());
        userData.put("userType", principal.getUserType());
        userData.put("role", principal.getRole());
        userData.put("name", principal.getName());
        userData.put("email", principal.getEmail());
        userData.put("mobile", principal.getMobile());
        userData.put("restaurantId", principal.getRestaurantId());
        userData.put("restaurants", principal.getRestaurants());
        userData.put("isSuperAdmin", principal.isSuperAdmin());

        return ResponseEntity.ok(
                new Response(Collections.singletonList(userData), false, "User retrieved successfully"));
    }

    public record RefreshTokenRequest(String refreshToken) {}
}
