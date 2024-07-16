package com.hyp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.hyp.util.EncryptionUtils;

import jakarta.annotation.PostConstruct;

@Configuration
public class EncryptionConfig {

	@Value("${app.secret.key}")
	private String appSecretKey;

	@PostConstruct
	public void init() {
		EncryptionUtils.setKey(appSecretKey);
	}
}
