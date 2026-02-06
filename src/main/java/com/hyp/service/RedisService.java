package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.params.SetParams;

/**
 * Redis service using JedisPooled for all operations.
 * This consolidates all Redis operations into a single connection pool,
 * reducing connection usage compared to using both RedisTemplate and JedisPooled.
 */
@Slf4j
@Component
public class RedisService {

    @Autowired
    private JedisPooled jedis;

    @Autowired
    private ObjectMapper objectMapper;

    public boolean isNotificationServiceEnabled() {
        String value = jedis.get("notification:service:enabled");
        return Boolean.parseBoolean(value);
    }

    public String getAlertUsers() {
        return jedis.get("whatsappAlert");
    }

    public String getInternalUsers() {
        return jedis.get("internalUsers");
    }

    /**
     * Get a string value from Redis.
     */
    public Optional<String> getRedisData(String key) {
        try {
            String value = jedis.get(key);
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
            jedis.setex(key, ttlSeconds, jsonValue);
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
            jedis.incr(key);
        } catch (Exception e) {
            log.error("Failed to increment Redis key: {}", key, e);
        }
    }

    /**
     * Set JSON data using Redis JSON module.
     */
    @Async
    public void setRedisJsonData(String key, Object data, long ttlSeconds) {
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            jedis.jsonSet(key, Path2.ROOT_PATH, jsonString);
            jedis.expire(key, ttlSeconds);
            log.info("Redis JSON Data set operation completed for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to set Redis JSON data for key: {}", key, e);
        }
    }

    /**
     * Get JSON data using Redis JSON module.
     */
    public <T> Optional<T> getRedisJsonData(String key, Class<T> valueType) {
        try {
            Object jsonElement = jedis.jsonGet(key);
            if (jsonElement != null) {
                String json = objectMapper.writeValueAsString(jsonElement);
                T data = objectMapper.readValue(json, valueType);
                return Optional.of(data);
            }
        } catch (Exception e) {
            log.error("Failed to get Redis JSON data for key: {}", key, e);
        }
        return Optional.empty();
    }

    /**
     * Remove a key from Redis.
     */
    @Async
    public void removeRedisData(String key) {
        try {
            long deleted = jedis.del(key);
            if (deleted > 0) {
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
            return jedis.exists(key);
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
            jedis.expire(key, ttlSeconds);
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
            jedis.setex(key, ttlSeconds, value);
            log.debug("Redis sync set operation completed for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to set Redis data synchronously for key: {}", key, e);
        }
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        try {
            SetParams params = new SetParams().nx().ex((int) ttl.getSeconds());

            String result = jedis.set(key, value, params);
            return "OK".equals(result);

        } catch (Exception e) {
            log.error("Redis SET NX failed for key={}", key, e);
            throw e;
        }
    }
}
