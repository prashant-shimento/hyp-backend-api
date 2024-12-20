package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisService {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	public boolean isNotificationServiceEnabled() {
	    String value = redisTemplate.opsForValue().get("notification:service:enabled");
	    return Boolean.parseBoolean(value);
	}
	
	public String getAlertUsers() {
	    return redisTemplate.opsForValue().get("whatsappAlert");
	}
	
	public String getInternalUsers() {
	    return redisTemplate.opsForValue().get("internalUsers");
	}
}	
