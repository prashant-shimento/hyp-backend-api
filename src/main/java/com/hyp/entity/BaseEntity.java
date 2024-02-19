package com.hyp.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Document
public abstract class BaseEntity implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@Field("id")
	private String id;
	
	@Field("created_at")
	@CreatedDate
	private LocalDateTime createdAt;
	
	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;
}
