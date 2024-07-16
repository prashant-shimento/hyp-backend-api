package com.hyp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiConfig {

	private String baseUrl;
	
	private String token;

	private String secret;
	
	private String key;
	
	private String username;
	
	private String password;
}
