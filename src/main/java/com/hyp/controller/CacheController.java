package com.hyp.controller;

import com.hyp.exception.EntityNotFoundException;
import com.hyp.response.Response;
import com.hyp.service.CacheService;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/cache")
public class CacheController {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private LettuceConnectionFactory lettuceConnectionFactory;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheService cacheService;

    @GetMapping
    public ResponseEntity<Response> listAllCaches() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        return ResponseEntity.ok(new Response(List.of(cacheNames), false, "All cache names listed"));
    }

    @GetMapping("/{cacheName}")
    public ResponseEntity<Response> getCacheContent(@PathVariable String cacheName) throws EntityNotFoundException {
        Cache cache = Optional.ofNullable(cacheManager.getCache(cacheName))
                .orElseThrow(() -> new EntityNotFoundException("Cache", cacheName));

        Object nativeCache = cache.getNativeCache();

        if (nativeCache instanceof ConcurrentMap) {
            @SuppressWarnings("unchecked")
            ConcurrentMap<Object, Object> cacheMap = (ConcurrentMap<Object, Object>) nativeCache;

            return ResponseEntity.ok(new Response(List.of(cacheMap), false, "Cache content fetched successfully"));
        }

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(new Response(
                        null,
                        true,
                        "Unsupported cache backend: " + nativeCache.getClass().getName()));
    }

    @PostMapping("/{cacheName}")
    public ResponseEntity<Response> createCache(@PathVariable String cacheName) {
        cacheManager.getCache(cacheName);
        return ResponseEntity.ok(new Response(null, false, "Cache '" + cacheName + "' created successfully"));
    }

    @PutMapping("/{cacheName}")
    public ResponseEntity<Response> addOrUpdateCacheEntry(
            @PathVariable String cacheName, @RequestBody Map<Object, Object> entries) throws EntityNotFoundException {

        Cache cache = Optional.ofNullable(cacheManager.getCache(cacheName))
                .orElseThrow(() -> new EntityNotFoundException("Cache", cacheName));

        entries.forEach(cache::put);

        return ResponseEntity.ok(new Response(null, false, "Cache entries added/updated"));
    }

    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Response> clearCache(@PathVariable String cacheName) throws EntityNotFoundException {
        Cache cache = Optional.ofNullable(cacheManager.getCache(cacheName))
                .orElseThrow(() -> new EntityNotFoundException("Cache", cacheName));

        cache.clear();
        return ResponseEntity.ok(new Response(null, false, "Cache '" + cacheName + "' cleared successfully"));
    }

    @DeleteMapping
    public ResponseEntity<Response> clearAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        return ResponseEntity.ok(new Response(null, false, "All caches cleared successfully"));
    }

    /**
     * Get Redis connection pool statistics.
     * Lettuce pool configuration and connection info.
     */
    @GetMapping("/redis/pool-stats")
    public ResponseEntity<Response> getRedisPoolStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        try {
            Map<String, Object> poolStats = new LinkedHashMap<>();
            poolStats.put("description", "Lettuce connection pool for all Redis operations");
            poolStats.put("hostName", lettuceConnectionFactory.getHostName());
            poolStats.put("port", lettuceConnectionFactory.getPort());

            if (lettuceConnectionFactory.getClientConfiguration()
                    instanceof LettucePoolingClientConfiguration poolingConfig) {
                var poolConfig = poolingConfig.getPoolConfig();
                poolStats.put("maxTotal", poolConfig.getMaxTotal());
                poolStats.put("maxIdle", poolConfig.getMaxIdle());
                poolStats.put("minIdle", poolConfig.getMinIdle());
                poolStats.put("maxWaitMillis", poolConfig.getMaxWaitDuration().toMillis());
                poolStats.put(
                        "commandTimeout", poolingConfig.getCommandTimeout().toMillis());
            }

            stats.put("lettucePool", poolStats);

            return ResponseEntity.ok(
                    new Response(Collections.singletonList(stats), false, "Redis pool stats retrieved successfully"));

        } catch (Exception e) {
            log.error("Error getting Redis pool stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response(null, true, "Failed to get Redis pool stats: " + e.getMessage()));
        }
    }

    /**
     * Get partner IDs with server correction enabled.
     */
    @GetMapping("/config/server-correction")
    public ResponseEntity<Response> getServerCorrectionPartners() {
        Set<String> partnerIds = cacheService.getServerCorrectionPartnerIds();
        return ResponseEntity.ok(
                new Response(Collections.singletonList(partnerIds), false, "Server correction partner IDs retrieved"));
    }

    /**
     * Set partner IDs with server correction enabled.
     * Body: { "partnerIds": ["partner1", "partner2"] }
     */
    @PutMapping("/config/server-correction")
    public ResponseEntity<Response> setServerCorrectionPartners(@RequestBody Map<String, Set<String>> body) {
        Set<String> partnerIds = body.getOrDefault("partnerIds", Set.of());
        cacheService.setServerCorrectionPartnerIds(partnerIds);
        return ResponseEntity.ok(
                new Response(Collections.singletonList(partnerIds), false, "Server correction partner IDs updated"));
    }

    /**
     * Get Redis connection health check.
     */
    @GetMapping("/redis/health")
    public ResponseEntity<Response> getRedisHealth() {
        Map<String, Object> health = new LinkedHashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            String pingResponse =
                    stringRedisTemplate.getConnectionFactory().getConnection().ping();
            long latency = System.currentTimeMillis() - startTime;

            health.put("status", "UP");
            health.put("ping", pingResponse);
            health.put("latencyMs", latency);
            health.put("host", lettuceConnectionFactory.getHostName());
            health.put("port", lettuceConnectionFactory.getPort());

            return ResponseEntity.ok(new Response(Collections.singletonList(health), false, "Redis is healthy"));

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new Response(Collections.singletonList(health), true, "Redis is unhealthy"));
        }
    }
}
