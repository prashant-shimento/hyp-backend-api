package com.hyp.service;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MockService {
	
	@Autowired
	private ObjectMapper objectMapper;

	public <T> T readMock(String filename, Class<T> clazz) {
	    try (InputStream inputStream = new ClassPathResource("mocks/" + filename).getInputStream()) {
	        log.warn("Returning mock response from file: {}", filename);
	        return objectMapper.readValue(inputStream, clazz);
	    } catch (IOException e) {
	        log.error("Failed to load mock file: {}", filename, e);
	        throw new RuntimeException("Unable to read mock: " + filename, e);
	    }
	}
}
