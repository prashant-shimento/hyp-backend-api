package com.hyp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hyp.util.EncryptionUtils;

@Configuration
public class EncryptionConfig {

	@Value("${app.secret.key}")
	private String appSecretKey;

	@Bean
	EncryptionUtils encryptionUtils() {
	    EncryptionUtils.setKey(appSecretKey);
	    return new EncryptionUtils();
	}
	
	@Bean
	PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
}
