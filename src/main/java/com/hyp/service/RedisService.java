package com.hyp.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisService {

	@Autowired
	private RedisTemplate<String, String> redisStringTemplate;

	@Autowired
	private RedisTemplate<String, Object> redisObjectTemplate;

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
	public void removeRedisKey(String key) {
		try {
			Boolean isDeleted = redisObjectTemplate.delete(key);
			if (Boolean.TRUE.equals(isDeleted)) {
				log.info("Successfully removed key: {}", key);
			} else {
				log.warn("Key not found or not removed: {}", key);
			}
		} catch (Exception e) {
			log.error("Failed to delete Redis key: {}", key, e);
		}
	}
}
