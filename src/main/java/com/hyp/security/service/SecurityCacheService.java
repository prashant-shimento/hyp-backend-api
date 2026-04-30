package com.hyp.security.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.entity.IntegrationKey;
import com.hyp.entity.Permission;
import com.hyp.entity.Role;
import com.hyp.repository.AccessPolicyRepository;
import com.hyp.repository.IntegrationKeyRepository;
import com.hyp.repository.PermissionRepository;
import com.hyp.repository.RoleRepository;
import com.hyp.service.CacheService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * L1/L2 caching service for security-related data (roles, permissions, policies).
 * Uses CacheService for Caffeine (L1, 5 min) + Redis (L2, 10 min) with self-healing loaders.
 * Pub/sub invalidation uses StringRedisTemplate directly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityCacheService {

    static final String ROLES_CACHE = "security:roles";
    static final String PERMS_CACHE = "security:permissions";
    static final String POLICIES_CACHE = "security:policies";
    static final String APIKEYS_CACHE = "security:apikeys";

    public static final String CACHE_INVALIDATION_CHANNEL = "security:cache:invalidation";

    private final CacheService cacheService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccessPolicyRepository accessPolicyRepository;
    private final IntegrationKeyRepository integrationKeyRepository;

    // ==================== Role Cache Operations ====================

    /**
     * Store all roles in L1/L2 cache
     */
    public <T> void cacheRoles(Map<String, T> roles) {
        try {
            cacheService.put(ROLES_CACHE, "all", roles);
            log.debug("Cached {} roles in L1/L2 cache", roles.size());
        } catch (Exception e) {
            log.error("Failed to cache roles", e);
        }
    }

    /**
     * Get all roles from L1/L2 cache, loading from DB on cold read
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getCachedRoles(Class<T> valueType) {
        try {
            Object cached = cacheService.getOrLoad(ROLES_CACHE, "all", Map.class, this::loadRolesFromDB);
            if (cached != null) {
                Map<String, Object> rawMap =
                        objectMapper.convertValue(cached, new TypeReference<Map<String, Object>>() {});
                Map<String, T> result = new HashMap<>();
                for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    result.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), valueType));
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to get cached roles", e);
        }
        return Collections.emptyMap();
    }

    /**
     * Check if roles cache exists
     */
    public boolean hasRolesCache() {
        try {
            return cacheService.exists(ROLES_CACHE, "all");
        } catch (Exception e) {
            log.error("Failed to check roles cache existence", e);
            return false;
        }
    }

    // ==================== Permission Cache Operations ====================

    /**
     * Store all permissions in L1/L2 cache
     */
    public <T> void cachePermissions(Map<String, T> permissions) {
        try {
            cacheService.put(PERMS_CACHE, "all", permissions);
            log.debug("Cached {} permissions in L1/L2 cache", permissions.size());
        } catch (Exception e) {
            log.error("Failed to cache permissions", e);
        }
    }

    /**
     * Get all permissions from L1/L2 cache, loading from DB on cold read
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getCachedPermissions(Class<T> valueType) {
        try {
            Object cached = cacheService.getOrLoad(PERMS_CACHE, "all", Map.class, this::loadPermissionsFromDB);
            if (cached != null) {
                Map<String, Object> rawMap =
                        objectMapper.convertValue(cached, new TypeReference<Map<String, Object>>() {});
                Map<String, T> result = new HashMap<>();
                for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    result.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), valueType));
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to get cached permissions", e);
        }
        return Collections.emptyMap();
    }

    /**
     * Check if permissions cache exists
     */
    public boolean hasPermissionsCache() {
        try {
            return cacheService.exists(PERMS_CACHE, "all");
        } catch (Exception e) {
            log.error("Failed to check permissions cache existence", e);
            return false;
        }
    }

    // ==================== Policy Cache Operations ====================

    /**
     * Store all policies in L1/L2 cache
     */
    public <T> void cachePolicies(List<T> policies) {
        try {
            cacheService.put(POLICIES_CACHE, "all", policies);
            log.debug("Cached {} policies in L1/L2 cache", policies.size());
        } catch (Exception e) {
            log.error("Failed to cache policies", e);
        }
    }

    /**
     * Get all policies from L1/L2 cache, loading from DB on cold read
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getCachedPolicies(Class<T> valueType) {
        try {
            Object cached = cacheService.getOrLoad(POLICIES_CACHE, "all", List.class, this::loadPoliciesFromDB);
            if (cached != null) {
                List<Object> rawList = objectMapper.convertValue(cached, new TypeReference<List<Object>>() {});
                List<T> result = new ArrayList<>();
                for (Object item : rawList) {
                    result.add(objectMapper.convertValue(item, valueType));
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Failed to get cached policies", e);
        }
        return Collections.emptyList();
    }

    /**
     * Check if policies cache exists
     */
    public boolean hasPoliciesCache() {
        try {
            return cacheService.exists(POLICIES_CACHE, "all");
        } catch (Exception e) {
            log.error("Failed to check policies cache existence", e);
            return false;
        }
    }

    // ==================== Cache Invalidation ====================

    /**
     * Clear all security caches
     */
    public void clearAllCaches() {
        try {
            cacheService.evict(ROLES_CACHE, "all");
            cacheService.evict(PERMS_CACHE, "all");
            cacheService.evict(POLICIES_CACHE, "all");
            log.info("Cleared all security caches");
        } catch (Exception e) {
            log.error("Failed to clear security caches", e);
        }
    }

    /**
     * Clear only role-related caches
     */
    public void clearRoleCaches() {
        try {
            cacheService.evict(ROLES_CACHE, "all");
            log.info("Cleared role caches");
        } catch (Exception e) {
            log.error("Failed to clear role caches", e);
        }
    }

    /**
     * Clear only permission cache
     */
    public void clearPermissionCache() {
        try {
            cacheService.evict(PERMS_CACHE, "all");
            log.info("Cleared permission cache");
        } catch (Exception e) {
            log.error("Failed to clear permission cache", e);
        }
    }

    /**
     * Clear only policy cache
     */
    public void clearPolicyCache() {
        try {
            cacheService.evict(POLICIES_CACHE, "all");
            log.info("Cleared policy cache");
        } catch (Exception e) {
            log.error("Failed to clear policy cache", e);
        }
    }

    /**
     * Publish cache invalidation event to notify all instances
     */
    public void publishCacheInvalidation(CacheInvalidationType type) {
        try {
            stringRedisTemplate.convertAndSend(CACHE_INVALIDATION_CHANNEL, type.name());
            log.info("Published cache invalidation event: {}", type);
        } catch (Exception e) {
            log.error("Failed to publish cache invalidation event", e);
        }
    }

    // ==================== Integration Key Cache Operations ====================

    /**
     * Get IntegrationKey from L1/L2 cache, falling back to DB on cold read.
     * Returns empty if the key does not exist or is inactive.
     */
    public Optional<IntegrationKey> getOrLoadIntegrationKey(String apiKey) {
        try {
            IntegrationKey integrationKey =
                    cacheService.getOrLoad(APIKEYS_CACHE, apiKey, IntegrationKey.class, () -> integrationKeyRepository
                            .findByApiKeyAndActiveTrue(apiKey)
                            .orElse(null));
            return Optional.ofNullable(integrationKey);
        } catch (Exception e) {
            log.error("Failed to load IntegrationKey from cache for key: {}", maskApiKey(apiKey), e);
            return Optional.empty();
        }
    }

    /**
     * Evict a single IntegrationKey entry (call after update or revocation).
     */
    public void evictIntegrationKey(String apiKey) {
        cacheService.evict(APIKEYS_CACHE, apiKey);
    }

    // ==================== Private Loader Helpers ====================

    @SuppressWarnings("unchecked")
    private Map loadRolesFromDB() {
        Map<String, Role> map = new HashMap<>();
        roleRepository.findByActiveTrue().forEach(r -> map.put(r.getName(), r));
        return map;
    }

    @SuppressWarnings("unchecked")
    private Map loadPermissionsFromDB() {
        Map<String, Permission> map = new HashMap<>();
        permissionRepository.findByActiveTrue().forEach(p -> map.put(p.getName(), p));
        return map;
    }

    @SuppressWarnings("unchecked")
    private List loadPoliciesFromDB() {
        return accessPolicyRepository.findByActiveTrueOrderByPriorityDesc();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "***";
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Cache invalidation types
     */
    public enum CacheInvalidationType {
        ROLES,
        PERMISSIONS,
        POLICIES,
        ALL
    }
}
