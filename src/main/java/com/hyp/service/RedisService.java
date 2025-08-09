package com.hyp.service;

import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path2;

@Slf4j
@Component
public class RedisService {

	@Autowired
	private RedisTemplate<String, String> redisStringTemplate;

	@Autowired
	private RedisTemplate<String, Object> redisObjectTemplate;

	@Autowired
	private JedisPooled jedis;

	@Autowired
	private ObjectMapper objectMapper;

	public boolean isNotificationServiceEnabled() {
		String value = redisStringTemplate.opsForValue().get("notification:service:enabled");
		return Boolean.parseBoolean(value);
	}

	public String getAlertUsers() {
		return redisStringTemplate.opsForValue().get("whatsappAlert");
	}

	public String getInternalUsers() {
		return redisStringTemplate.opsForValue().get("internalUsers");
	}

	@Async
	public void setRedisData(String key, Object value, long ttl) {
		try {
			redisObjectTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
			log.info("Redis set operation completed for key: {}", key);
		} catch (Exception e) {
			log.error("Failed to set Redis data for key: {}", key, e);
		}
	}

	@Async
	public void increment(String key) {
		redisStringTemplate.opsForValue().increment(key);
	}

	@Async
	public void setRedisJsonData(String key, Object data, long ttl) {
		try {
			String jsonString = objectMapper.writeValueAsString(data);
			jedis.jsonSet(key, Path2.ROOT_PATH, jsonString);
			jedis.expire(key, ttl);
			log.info("Redis JSON Data set operation completed for key: {}", key);
		} catch (Exception e) {
			log.error("Failed to set Redis JSON data for key: {}", key, e);
		}
	}

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

	@Async
	public void removeRedisData(String key) {
		try {
			Boolean isDeleted = redisObjectTemplate.delete(key);
			if (isDeleted) {
				log.info("Successfully removed key: {}", key);
			} else {
				log.warn("Key not found or not removed: {}", key);
			}
		} catch (Exception e) {
			log.error("Failed to delete Redis key: {}", key, e);
		}
	}

	public Optional<String> getRedisData(String key) {
		try {
			String value = redisStringTemplate.opsForValue().get(key);
			return Optional.ofNullable(value);
		} catch (Exception e) {
			log.error("Error fetching value from Redis for key: {}", key, e);
			return Optional.empty();
		}
	}

}
