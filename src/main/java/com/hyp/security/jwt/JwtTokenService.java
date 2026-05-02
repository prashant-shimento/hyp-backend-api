package com.hyp.security.jwt;

import com.hyp.entity.Customer;
import com.hyp.entity.RefreshToken;
import com.hyp.entity.User;
import com.hyp.repository.RefreshTokenRepository;
import com.hyp.security.exception.JwtAuthenticationException;
import com.hyp.security.principal.UserPrincipal;
import com.hyp.service.CustomerService;
import com.hyp.service.UserService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerService customerService;
    private final UserService userService;

    private static final int MAX_REFRESH_TOKENS_PER_USER = 5;

    /**
     * Create both access and refresh tokens for a user
     */
    public Map<String, Object> createTokenPair(UserPrincipal principal, String deviceInfo, String ipAddress) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        // Store hashed refresh token in database
        saveRefreshToken(refreshToken, principal, deviceInfo, ipAddress);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("tokenType", "Bearer");
        tokens.put("expiresIn", jwtTokenProvider.getAccessTokenExpirationMs() / 1000);

        return tokens;
    }

    /**
     * Refresh an access token using a valid refresh token.
     * Loads fresh user data from DB so the new access token always has up-to-date claims.
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        // Validate the refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new JwtAuthenticationException("Invalid refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new JwtAuthenticationException("Token is not a refresh token");
        }

        // Check if refresh token exists in database and is not revoked
        String hashedToken = hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository
                .findByToken(hashedToken)
                .orElseThrow(() -> new JwtAuthenticationException("Refresh token not found or already revoked"));

        if (!storedToken.isValid()) {
            throw new JwtAuthenticationException("Refresh token is expired or revoked");
        }

        // Build a fresh principal from DB instead of stale token claims
        UserPrincipal principal = buildPrincipalFromDb(storedToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);

        Map<String, Object> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("tokenType", "Bearer");
        tokens.put("expiresIn", jwtTokenProvider.getAccessTokenExpirationMs() / 1000);

        return tokens;
    }

    /**
     * Build a fresh UserPrincipal by loading user data from DB.
     * Uses the stored token's userId, userType, and restaurantId.
     */
    private UserPrincipal buildPrincipalFromDb(RefreshToken storedToken) {
        String userId = storedToken.getUserId();
        String userType = storedToken.getUserType();

        if ("CUSTOMER".equals(userType)) {
            Customer customer = customerService.findById(userId);
            if (customer == null) {
                throw new JwtAuthenticationException("Customer not found");
            }
            return UserPrincipal.builder()
                    .userId(customer.getId())
                    .userType("CUSTOMER")
                    .role("CUSTOMER")
                    .mobile(customer.getMobile())
                    .name(customer.getName())
                    .restaurantId(storedToken.getRestaurantId())
                    .restaurants(customer.getRestaurants())
                    .build();
        }

        // USER type (RESTAURANT_USER, RESTAURANT_ADMIN, PLATFORM_USER, etc.)
        User user = userService.findById(userId);
        if (user == null) {
            throw new JwtAuthenticationException("User not found");
        }
        String role = user.getRole() != null ? user.getRole() : storedToken.getRole();
        return UserPrincipal.builder()
                .userId(user.getId())
                .userType("USER")
                .role(role)
                .email(user.getEmail())
                .name(user.getName())
                .mobile(user.getMobile())
                .restaurantId(storedToken.getRestaurantId())
                .partnerId(user.getPartnerId())
                .restaurants(user.getRestaurantIds() != null ? user.getRestaurantIds() : Collections.emptySet())
                .build();
    }

    /**
     * Revoke a specific refresh token
     */
    public void revokeRefreshToken(String refreshToken) {
        String hashedToken = hashToken(refreshToken);
        refreshTokenRepository.findByToken(hashedToken).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
            log.info("Revoked refresh token for user: {}", token.getUserId());
        });
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     */
    public void revokeAllUserTokens(String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
        });
        refreshTokenRepository.saveAll(tokens);
        log.info("Revoked all refresh tokens for user: {}", userId);
    }

    /**
     * Save refresh token to database (hashed)
     */
    private void saveRefreshToken(String refreshToken, UserPrincipal principal, String deviceInfo, String ipAddress) {
        // Limit the number of active refresh tokens per user
        long activeTokenCount = refreshTokenRepository.countByUserIdAndRevokedFalse(principal.getUserId());
        if (activeTokenCount >= MAX_REFRESH_TOKENS_PER_USER) {
            // Revoke oldest tokens
            List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(principal.getUserId());
            tokens.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .limit(tokens.size() - MAX_REFRESH_TOKENS_PER_USER + 1)
                    .forEach(token -> {
                        token.setRevoked(true);
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    });
        }

        String hashedToken = hashToken(refreshToken);
        LocalDateTime expiresAt =
                LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationMs() / 1000);

        RefreshToken tokenEntity = RefreshToken.builder()
                .token(hashedToken)
                .userId(principal.getUserId())
                .userType(principal.getUserType())
                .role(principal.getRole())
                .restaurantId(principal.getRestaurantId())
                .expiresAt(expiresAt)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.save(tokenEntity);
        log.debug("Saved refresh token for user: {}", principal.getUserId());
    }

    /**
     * Hash the refresh token for secure storage
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    /**
     * Validate refresh token exists and is not revoked
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return false;
        }
        String hashedToken = hashToken(refreshToken);
        return refreshTokenRepository
                .findByToken(hashedToken)
                .map(RefreshToken::isValid)
                .orElse(false);
    }
}
