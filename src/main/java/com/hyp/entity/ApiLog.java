package com.hyp.entity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor 
@Getter
@Setter
@ToString
@Builder
@Document(collection = "api_logs")
public class ApiLog {

	@Id
	private String id;
	
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
	private int status;
	
	@Field("created_at")
	@CreatedDate
	private LocalDateTime createdAt;

	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;

	@Field("restaurant_id")
	private String restaurantId;

}

