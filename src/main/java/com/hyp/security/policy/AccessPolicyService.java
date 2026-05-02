package com.hyp.security.policy;

import com.hyp.entity.AccessPolicy;
import com.hyp.repository.AccessPolicyRepository;
import com.hyp.security.principal.RestaurantContext;
import com.hyp.security.principal.UserPrincipal;
import com.hyp.security.service.RoleService;
import com.hyp.security.service.SecurityCacheService;
import com.hyp.security.service.SecurityCacheService.CacheInvalidationType;
import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
public class AccessPolicyService {

    private final AccessPolicyRepository accessPolicyRepository;
    private final RoleService roleService;
    private final PathMatcher pathMatcher;
    private final SecurityCacheService securityCacheService;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * Refresh the policy cache from database and store in Redis
     */
    public void refreshCache() {
        log.info("Refreshing access policy cache...");

        List<AccessPolicy> policies = accessPolicyRepository.findByActiveTrueOrderByPriorityDesc();

        // Store in Redis
        securityCacheService.cachePolicies(policies);

        log.info("Access policy cache refreshed. Loaded {} policies", policies.size());
    }

    /**
     * Refresh cache and notify other instances
     */
    public void refreshCacheAndNotify() {
        refreshCache();
        securityCacheService.publishCacheInvalidation(CacheInvalidationType.POLICIES);
    }

    /**
     * Evaluate a request against access policies
     */
    public PolicyDecision evaluate(String path, String method, UserPrincipal principal) {
        // Get policies from Redis cache
        List<AccessPolicy> policyCache = securityCacheService.getCachedPolicies(AccessPolicy.class);

        // Find matching policies
        List<AccessPolicy> matchingPolicies = findMatchingPolicies(path, method, policyCache);

        if (matchingPolicies.isEmpty()) {
            log.debug("No policy found for path: {} method: {} - denying by default", path, method);
            return PolicyDecision.deny("Access to this resource is not permitted.");
        }

        for (AccessPolicy policy : matchingPolicies) {
            // 1. Check if endpoint is public
            if (policy.isPublic()) {
                log.debug("Public access allowed for path: {}", path);

                if (policy.isRestaurantScoped()) {
                    String restaurantId = RestaurantContext.getRestaurantId();

                    // Restaurant context is mandatory for scoped resources
                    if (restaurantId == null || restaurantId.isBlank()) {
                        return PolicyDecision.deny("X-Restaurant-Id header is required");
                    }

                    // If user is authenticated, validate restaurant access
                    if (principal != null && !principal.canAccessRestaurant(restaurantId)) {
                        return PolicyDecision.deny("You don't have access to this restaurant.");
                    }
                }
                return PolicyDecision.allow(policy.isRestaurantScoped(), policy.isOwnerOnly());
            }

            // 2. No principal but auth required
            if (principal == null) {
                log.debug("Authentication required for path: {}", path);
                return PolicyDecision.requireAuth();
            }

            // 3. Super admin bypass - skip all policy checks
            if (roleService.isSuperAdmin(principal.getRole())) {
                log.debug("Super admin access granted for path: {}", path);
                return PolicyDecision.allow();
            }

            // 4. Check permissions (user must have at least one of the required permissions)
            Set<String> userEffectivePermissions = roleService.getEffectivePermissions(principal.getRole());
            boolean hasPermission = policy.getPermissions() == null
                    || policy.getPermissions().isEmpty()
                    || policy.getPermissions().stream().anyMatch(userEffectivePermissions::contains);

            if (hasPermission) {
                // 5. Restaurant scope check
                if (policy.isRestaurantScoped()) {
                    String reqRestaurantId = RestaurantContext.getRestaurantId();
                    if (reqRestaurantId == null || reqRestaurantId.isBlank()) {
                        return PolicyDecision.deny("X-Restaurant-Id header is required");
                    }

                    if (!principal.canAccessRestaurant(reqRestaurantId)) {
                        log.debug(
                                "Restaurant access denied for user: {} restaurant: {}",
                                principal.getUserId(),
                                reqRestaurantId);
                        return PolicyDecision.deny("You don't have access to this restaurant.");
                    }
                }

                // 6. Owner check (deferred to service layer)
                if (policy.isOwnerOnly()) {
                    log.debug("Owner check required for path: {}", path);
                    return PolicyDecision.requireOwnerCheck(policy.isRestaurantScoped());
                }

                log.debug("Access granted for user: {} path: {}", principal.getUserId(), path);
                return PolicyDecision.allow(policy.isRestaurantScoped(), false);
            }
        }

        // No matching policy allowed access
        log.debug("Access denied - no matching permission for path: {} user role: {}", path, principal.getRole());
        return PolicyDecision.deny("You don't have permission to perform this operation.");
    }

    /**
     * Find all policies that match the given path and method
     */
    private List<AccessPolicy> findMatchingPolicies(String path, String method, List<AccessPolicy> policyCache) {
        return policyCache.stream()
                .filter(policy -> pathMatcher.matches(policy.getResource(), path))
                .filter(policy -> pathMatcher.matchesOperation(policy.getOperation(), method))
                .sorted(Comparator.comparingInt(AccessPolicy::getPriority)
                        .reversed()
                        .thenComparingInt(p -> -pathMatcher.getPatternSpecificity(p.getResource())))
                .collect(Collectors.toList());
    }

    /**
     * Get all policies from cache
     */
    public List<AccessPolicy> getAllPolicies() {
        return List.copyOf(securityCacheService.getCachedPolicies(AccessPolicy.class));
    }

    /**
     * Create a new policy
     */
    public AccessPolicy createPolicy(AccessPolicy policy) {
        AccessPolicy saved = accessPolicyRepository.save(policy);
        refreshCacheAndNotify();
        return saved;
    }

    /**
     * Update an existing policy
     */
    public AccessPolicy updatePolicy(AccessPolicy policy) {
        AccessPolicy saved = accessPolicyRepository.save(policy);
        refreshCacheAndNotify();
        return saved;
    }

    /**
     * Delete a policy
     */
    public boolean deletePolicy(String policyId) {
        if (accessPolicyRepository.existsById(policyId)) {
            accessPolicyRepository.deleteById(policyId);
            refreshCacheAndNotify();
            return true;
        }
        return false;
    }
}
