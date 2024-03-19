package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosBaseRequest {

	@JsonProperty("app_key")
	private String appKey;

	@JsonProperty("app_secret")
	private String appSecret;

	@JsonProperty("access_token")
	private String accessToken;
}
