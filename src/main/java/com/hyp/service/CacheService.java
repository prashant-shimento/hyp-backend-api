package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CacheService {

    private static final String KEY_VERSION = "v1";
    private static final String NULL_MARKER = "__NULL__";

    private static final int L1_TTL_SECONDS = 300;
    private static final int L2_TTL_SECONDS = 600;
    private static final int NULL_TTL_SECONDS = 10;
    private static final int L1_MAX_SIZE = 1000;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Cache<String, Object>> l1Caches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info(
                "TwoLevelCacheService initialized | L1 TTL={}s | L2 TTL={}s | Version={}",
                L1_TTL_SECONDS,
                L2_TTL_SECONDS,
                KEY_VERSION);
    }

    public <T> T getOrLoad(String cacheName, String key, Class<T> type, Supplier<T> loader) {
        Objects.requireNonNull(cacheName, "cacheName must not be null");
        Objects.requireNonNull(key, "key must not be null");

        Cache<String, Object> l1Cache = getL1Cache(cacheName);

        // -------- L1 --------
        @SuppressWarnings("unchecked")
        T l1Value = (T) l1Cache.getIfPresent(key);
        if (l1Value != null) {
            log.trace("cache HIT L1 [{}/{}]", cacheName, key);
            return l1Value;
        }
        log.debug("cache MISS L1 [{}/{}] — checking L2 (Redis)", cacheName, key);

        // -------- SINGLE FLIGHT --------
        Object lock = keyLocks.computeIfAbsent(buildKey(cacheName, key), k -> new Object());
        synchronized (lock) {
            try {
                // Double-check L1
                @SuppressWarnings("unchecked")
                T retryValue = (T) l1Cache.getIfPresent(key);
                if (retryValue != null) {
                    log.trace("cache HIT L1 [{}/{}] (after lock)", cacheName, key);
                    return retryValue;
                }

                // -------- L2 --------
                String redisKey = buildKey(cacheName, key);
                try {
                    String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
                    if (redisValue != null) {
                        if (NULL_MARKER.equals(redisValue)) {
                            log.debug("cache HIT L2 (null) [{}/{}]", cacheName, key);
                            return null;
                        }
                        T value = objectMapper.readValue(redisValue, type);
                        l1Cache.put(key, value);
                        log.debug("cache HIT L2 [{}/{}] — promoted to L1", cacheName, key);
                        return value;
                    }
                } catch (Exception e) {
                    log.warn("Redis read failed [{}]: {}", redisKey, e.getMessage());
                }

                // -------- DB --------
                log.debug("cache MISS L2 [{}/{}] — loading from DB", cacheName, key);
                long dbStart = System.currentTimeMillis();
                T loaded = loader.get();
                log.debug("cache DB load [{}/{}] took {}ms", cacheName, key, System.currentTimeMillis() - dbStart);

                if (loaded == null) {
                    cacheNull(cacheName, key);
                    return null;
                }

                putInBothCaches(cacheName, key, loaded);
                return loaded;

            } finally {
                keyLocks.remove(buildKey(cacheName, key));
            }
        }
    }

    public <T> void put(String cacheName, String key, T value) {
        if (value == null) {
            cacheNull(cacheName, key);
        } else {
            putInBothCaches(cacheName, key, value);
        }
    }

    public void evict(String cacheName, String key) {
        Cache<String, Object> l1Cache = getL1Cache(cacheName);
        l1Cache.invalidate(key);

        try {
            stringRedisTemplate.unlink(buildKey(cacheName, key));
        } catch (Exception e) {
            log.warn("Redis eviction failed [{}]: {}", key, e.getMessage());
        }
    }

    public boolean exists(String cacheName, String key) {
        Cache<String, Object> l1Cache = getL1Cache(cacheName);
        if (l1Cache.getIfPresent(key) != null) return true;

        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(cacheName, key)));
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Server Correction Feature Flag ====================

    private static final String SERVER_CORRECTION_CACHE = "config:orderCorrection";
    private static final String SERVER_CORRECTION_KEY = "partnerIds";

    /**
     * Check if server-side price correction is enabled for a given partner.
     * When enabled, the OrderValidator will override client-submitted amounts
     * with server-calculated values. When disabled, only mismatch warnings are logged.
     */
    public boolean isServerCorrectionEnabled(String partnerId) {
        if (partnerId == null) return false;
        Set<String> enabledPartners = getServerCorrectionPartnerIds();
        return enabledPartners.contains(partnerId);
    }

    /**
     * Get the set of partner IDs with server correction enabled.
     * Cached in L1/L2; returns empty set if not configured.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getServerCorrectionPartnerIds() {
        try {
            Set<String> cached = getOrLoad(SERVER_CORRECTION_CACHE, SERVER_CORRECTION_KEY, Set.class, HashSet::new);
            if (cached != null) {
                return cached;
            }
            return Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to load server correction partner IDs", e);
            return Collections.emptySet();
        }
    }

    /**
     * Set the partner IDs that have server correction enabled.
     * Called from admin endpoint; updates both L1 and L2 cache.
     */
    public void setServerCorrectionPartnerIds(Set<String> partnerIds) {
        put(SERVER_CORRECTION_CACHE, SERVER_CORRECTION_KEY, partnerIds != null ? partnerIds : new HashSet<>());
        log.info("Updated server correction partner IDs: {}", partnerIds);
    }

    /* ================= INTERNAL HELPERS ================= */

    private <T> void putInBothCaches(String cacheName, String key, T value) {
        Cache<String, Object> l1Cache = getL1Cache(cacheName);
        l1Cache.put(key, value);

        try {
            stringRedisTemplate
                    .opsForValue()
                    .set(
                            buildKey(cacheName, key),
                            objectMapper.writeValueAsString(value),
                            Duration.ofSeconds(L2_TTL_SECONDS));
        } catch (Exception e) {
            log.warn("Redis write failed [{}]: {}", key, e.getMessage());
        }
    }

    private void cacheNull(String cacheName, String key) {
        Cache<String, Object> l1Cache = getL1Cache(cacheName);
        l1Cache.invalidate(key);
        try {
            stringRedisTemplate
                    .opsForValue()
                    .set(buildKey(cacheName, key), NULL_MARKER, Duration.ofSeconds(NULL_TTL_SECONDS));
        } catch (Exception e) {
            log.warn("Redis NULL cache failed [{}]: {}", key, e.getMessage());
        }
    }

    private Cache<String, Object> getL1Cache(String cacheName) {
        return l1Caches.computeIfAbsent(cacheName, name -> Caffeine.newBuilder()
                .maximumSize(L1_MAX_SIZE)
                .expireAfterWrite(L1_TTL_SECONDS, TimeUnit.SECONDS)
                .recordStats()
                .build());
    }

    private String buildKey(String cacheName, String key) {
        return "cache:" + KEY_VERSION + ":" + cacheName + ":" + key;
    }
}
