package com.hyp.security.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDecision {

    private Result result;
    private String reason;
    private boolean restaurantScoped;
    private boolean ownerOnly;

    public enum Result {
        ALLOW,
        DENY,
        REQUIRE_AUTH,
        REQUIRE_OWNER_CHECK
    }

    public static PolicyDecision allow() {
        return PolicyDecision.builder().result(Result.ALLOW).build();
    }

    public static PolicyDecision allow(boolean restaurantScoped, boolean ownerOnly) {
        return PolicyDecision.builder()
                .result(ownerOnly ? Result.REQUIRE_OWNER_CHECK : Result.ALLOW)
                .restaurantScoped(restaurantScoped)
                .ownerOnly(ownerOnly)
                .build();
    }

    public static PolicyDecision deny(String reason) {
        return PolicyDecision.builder().result(Result.DENY).reason(reason).build();
    }

    public static PolicyDecision requireAuth() {
        return PolicyDecision.builder()
                .result(Result.REQUIRE_AUTH)
                .reason("Authentication required")
                .build();
    }

    public static PolicyDecision requireOwnerCheck(boolean restaurantScoped) {
        return PolicyDecision.builder()
                .result(Result.REQUIRE_OWNER_CHECK)
                .restaurantScoped(restaurantScoped)
                .ownerOnly(true)
                .build();
    }

    public boolean isAllowed() {
        return result == Result.ALLOW || result == Result.REQUIRE_OWNER_CHECK;
    }
}
