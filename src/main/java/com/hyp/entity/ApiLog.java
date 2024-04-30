package com.hyp.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.constants.Constants.ApiStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Document(collection = "api_logs")
public class ApiLog extends BaseEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Field(name = "name")
	private String name;

	@Field(name = "url")
	private String url;

	@Field(name = "type")
	private String type;

	@Field(name = "method")
	private String method;

	@Field(name = "request")
	private String request;

	@Field(name = "response")
	private String response;

	@Field(name = "request_time")
	private Instant requestTime;

	@Field(name = "response_time")
	private Instant responseTime;

	@Field(name = "duration")
	private Duration duration;

	@Field(name = "status")
	private ApiStatus status;

	public ApiLog(String name, String url, String type, String method, String request, String response,
			Instant requestTime, Instant responseTime, Duration duration, ApiStatus status) {
		super();
		this.setId(UUID.randomUUID().toString());
		this.name = name;
		this.url = url;
		this.type = type;
		this.method = method;
		this.request = request;
		this.response = response;
		this.requestTime = requestTime;
		this.responseTime = responseTime;
		this.duration = duration != null ? duration : Duration.ZERO;
		this.status = status;
	}

}

