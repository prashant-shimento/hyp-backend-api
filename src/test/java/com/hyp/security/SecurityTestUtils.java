package com.hyp.security;

import com.hyp.security.jwt.JwtTokenProvider;
import com.hyp.security.principal.UserPrincipal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utility class for generating test tokens and principals for security tests.
 */
@Component
@RequiredArgsConstructor
public class SecurityTestUtils {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Creates a customer token for testing.
     */
    public String createCustomerToken(String customerId, String restaurantId, Set<String> restaurants) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(customerId)
                .userType("CUSTOMER")
                .role("CUSTOMER")
                .mobile("9876543210")
                .name("Test Customer")
                .restaurantId(restaurantId)
                .restaurants(restaurants)
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }

    /**
     * Creates a restaurant user token for testing.
     */
    public String createRestaurantUserToken(String userId, String restaurantId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .userType("USER")
                .role("RESTAURANT_USER")
                .email("user@restaurant.com")
                .name("Test User")
                .restaurantId(restaurantId)
                .restaurants(Set.of(restaurantId))
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }

    /**
     * Creates a restaurant admin token for testing.
     */
    public String createRestaurantAdminToken(String userId, String restaurantId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .userType("USER")
                .role("RESTAURANT_ADMIN")
                .email("admin@restaurant.com")
                .name("Test Admin")
                .restaurantId(restaurantId)
                .restaurants(Set.of(restaurantId))
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }

    /**
     * Creates a platform admin token for testing.
     */
    public String createPlatformAdminToken(String userId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(userId)
                .userType("USER")
                .role("PLATFORM_ADMIN")
                .email("admin@platform.com")
                .name("Platform Admin")
                .isSuperAdmin(true)
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }

    /**
     * Creates an API partner token for testing.
     */
    public String createApiPartnerToken(String partnerId, String restaurantId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(partnerId)
                .userType("PARTNER")
                .role("API_PARTNER")
                .partnerId(partnerId)
                .restaurantId(restaurantId)
                .restaurants(Set.of(restaurantId))
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }

    /**
     * Creates an AI agent token for testing.
     */
    public String createAiAgentToken(String agentId, String restaurantId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(agentId)
                .userType("PARTNER")
                .role("AI_AGENT")
                .partnerId(agentId)
                .restaurantId(restaurantId)
                .restaurants(Set.of(restaurantId))
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }

    /**
     * Creates a POS partner token for testing.
     */
    public String createPosPartnerToken(String partnerId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(partnerId)
                .userType("PARTNER")
                .role("POS_PARTNER")
                .partnerId(partnerId)
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }

    /**
     * Creates a delivery partner token for testing.
     */
    public String createDeliveryPartnerToken(String partnerId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(partnerId)
                .userType("PARTNER")
                .role("DELIVERY_PARTNER")
                .partnerId(partnerId)
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }

    /**
     * Creates a payment partner token for testing.
     */
    public String createPaymentPartnerToken(String partnerId) {
        UserPrincipal principal = UserPrincipal.builder()
                .userId(partnerId)
                .userType("PARTNER")
                .role("PAYMENT_PARTNER")
                .partnerId(partnerId)
                .build();
        return jwtTokenProvider.generateAccessToken(principal);
    }
}
