package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisService {

	@Autowired
	private StringRedisTemplate redisTemplate;
}
