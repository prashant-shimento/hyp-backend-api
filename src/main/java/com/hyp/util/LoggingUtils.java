package com.hyp.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingUtils {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static <T> void logRequest(String action, T request) {
		try {
			String requestJson = objectMapper.writeValueAsString(request);
			log.info("{} Request: {}", action, requestJson);
		} catch (Exception e) {
			log.warn("Failed to log request for {}: {}", action, e.getMessage());
		}
	}

	public static <T> void logResponse(String action, T response) {
		try {
			String responseJson = objectMapper.writeValueAsString(response);
			log.info("{} Response: {}", action, responseJson);
		} catch (Exception e) {
			log.warn("Failed to log response for {}: {}", action, e.getMessage());
		}
	}
}
