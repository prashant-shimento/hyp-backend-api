package com.hyp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration for Kubernetes Multi-Pod Deployment.
 *
 * IMPORTANT: We use TWO-LEVEL CACHING strategy:
 *
 * 1. TwoLevelCacheService (Primary - for entities like Restaurant, Customer)
 *    - L1: Caffeine (per-pod, 30s TTL)
 *    - L2: Redis (shared across pods, 5min TTL)
 *    - Use this for data that needs consistency across pods
 *
 * 2. Spring @Cacheable with CaffeineCacheManager (Secondary - for other caches)
 *    - Used for paymentConfigCache and other non-critical caches
 *    - Acceptable for data that can tolerate pod-local inconsistency
 *
 * Why Two-Level Cache?
 * - Single-pod Caffeine: Fast but inconsistent across K8s pods
 * - Redis-only: Consistent but slower (network hop)
 * - Two-Level: Best of both - fast AND consistent
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * CacheManager for Spring @Cacheable annotations.
     * Used for non-entity caches like paymentConfigCache.
     *
     * NOTE: For entity caches (restaurants, customers),
     * use TwoLevelCacheService instead!
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Short TTL for pod-local caches to limit inconsistency
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(1, TimeUnit.MINUTES) // Short TTL!
                .recordStats());

        // Register caches that are OK with pod-local caching
        cacheManager.setCacheNames(java.util.List.of(
                "paymentConfigCache" // Payment config rarely changes
                ));

        log.info("Caffeine CacheManager configured for secondary caches: {}", cacheManager.getCacheNames());
        log.info("Primary caches (restaurants, customers) use TwoLevelCacheService");

        return cacheManager;
    }
}
