package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Redis service using Lettuce via StringRedisTemplate for all operations.
 * This consolidates all Redis operations through a single connection pool.
 */
@Slf4j
@Component
public class RedisService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public String getAlertUsers() {
        return stringRedisTemplate.opsForValue().get("whatsappAlert");
    }

    public String getInternalUsers() {
        return stringRedisTemplate.opsForValue().get("internalUsers");
    }

    /**
     * Get a string value from Redis.
     */
    public Optional<String> getRedisData(String key) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Error fetching value from Redis for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Set an object value in Redis with TTL (serialized as JSON).
     */
    @Async
    public void setRedisData(String key, Object value, long ttlSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, jsonValue, Duration.ofSeconds(ttlSeconds));
            log.info("Redis set operation completed for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to set Redis data for key: {}", key, e);
        }
    }

    /**
     * Increment a counter in Redis.
     */
    @Async
    public void increment(String key) {
        try {
            stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Failed to increment Redis key: {}", key, e);
        }
    }

    /** Synchronous increment returning the new value. */
    public long incrementAndGet(String key) {
        try {
            Long result = stringRedisTemplate.opsForValue().increment(key);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.error("Failed to incrementAndGet Redis key: {}", key, e);
            return 0L;
        }
    }

    /** Synchronous decrement returning the new value. */
    public long decrementAndGet(String key) {
        try {
            Long result = stringRedisTemplate.opsForValue().decrement(key);
            return result != null ? result : 0L;
        } catch (Exception e) {
            log.error("Failed to decrementAndGet Redis key: {}", key, e);
            return 0L;
        }
    }

    /** Set a string value with no expiry. */
    public void setRedisString(String key, String value) {
        try {
            stringRedisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Failed to set Redis string for key: {}", key, e);
        }
    }

    /**
     * Set JSON data in Redis.
     */
    @Async
    public void setRedisJsonData(String key, Object data, long ttlSeconds) {
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(key, jsonString, Duration.ofSeconds(ttlSeconds));
            log.info("Redis JSON Data set operation completed for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to set Redis JSON data for key: {}", key, e);
        }
    }

    /**
     * Get JSON data from Redis.
     */
    public <T> Optional<T> getRedisJsonData(String key, Class<T> valueType) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                T data = objectMapper.readValue(json, valueType);
                return Optional.of(data);
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null
                    && cause.getMessage() != null
                    && cause.getMessage().startsWith("WRONGTYPE")) {
                log.warn("Key '{}' has wrong Redis type (stale key), deleting for self-heal", key);
                stringRedisTemplate.delete(key);
            } else {
                log.error("Failed to get Redis JSON data for key: {}", key, e);
            }
        }
        return Optional.empty();
    }

    /**
     * Remove a key from Redis.
     */
    @Async
    public void removeRedisData(String key) {
        try {
            Boolean deleted = stringRedisTemplate.unlink(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Successfully removed key: {}", key);
            } else {
                log.warn("Key not found or not removed: {}", key);
            }
        } catch (Exception e) {
            log.error("Failed to delete Redis key: {}", key, e);
        }
    }

    /**
     * Check if a key exists in Redis.
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Error checking existence of Redis key: {}", key, e);
            return false;
        }
    }

    /**
     * Set expiry on an existing key.
     */
    @Async
    public void expire(String key, long ttlSeconds) {
        try {
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.error("Failed to set expiry on Redis key: {}", key, e);
        }
    }

    /**
     * Set a string value in Redis with TTL (synchronous).
     * Use this when immediate persistence matters (e.g., status tracking).
     */
    public void setRedisStringDataSync(String key, String value, long ttlSeconds) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
            log.debug("Redis sync set operation completed for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to set Redis data synchronously for key: {}", key, e);
        }
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Redis SET NX failed for key={}", key, e);
            throw e;
        }
    }
}
