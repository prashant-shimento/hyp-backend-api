package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

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

    private final JedisPooled jedis;
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
            return l1Value;
        }

        // -------- SINGLE FLIGHT --------
        Object lock = keyLocks.computeIfAbsent(buildKey(cacheName, key), k -> new Object());
        synchronized (lock) {
            try {
                // Double-check L1
                @SuppressWarnings("unchecked")
                T retryValue = (T) l1Cache.getIfPresent(key);
                if (retryValue != null) {
                    return retryValue;
                }

                // -------- L2 --------
                String redisKey = buildKey(cacheName, key);
                try {
                    String redisValue = jedis.get(redisKey);
                    if (redisValue != null) {
                        if (NULL_MARKER.equals(redisValue)) {
                            return null;
                        }
                        T value = objectMapper.readValue(redisValue, type);
                        l1Cache.put(key, value);
                        return value;
                    }
                } catch (Exception e) {
                    log.warn("Redis read failed [{}]: {}", redisKey, e.getMessage());
                }

                // -------- DB --------
                T loaded = loader.get();
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
            jedis.del(buildKey(cacheName, key));
        } catch (Exception e) {
            log.warn("Redis eviction failed [{}]: {}", key, e.getMessage());
        }
    }

    public boolean exists(String cacheName, String key) {
        Cache<String, Object> l1Cache = getL1Cache(cacheName);
        if (l1Cache.getIfPresent(key) != null) return true;

        try {
            return jedis.exists(buildKey(cacheName, key));
        } catch (Exception e) {
            return false;
        }
    }

    /* ================= INTERNAL HELPERS ================= */

    private <T> void putInBothCaches(String cacheName, String key, T value) {
        Cache<String, Object> l1Cache = getL1Cache(cacheName);
        l1Cache.put(key, value);

        try {
            jedis.setex(buildKey(cacheName, key), L2_TTL_SECONDS, objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            log.warn("Redis write failed [{}]: {}", key, e.getMessage());
        }
    }

    private void cacheNull(String cacheName, String key) {
        Cache<String, Object> l1Cache = getL1Cache(cacheName);
        l1Cache.invalidate(key);
        try {
            jedis.setex(buildKey(cacheName, key), NULL_TTL_SECONDS, NULL_MARKER);
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
