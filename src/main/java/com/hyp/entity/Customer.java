package com.hyp.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.annotation.GenerateId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Document(collection = "customers")
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Customer {
	
	@Id
	@GenerateId(sequenceName = "customer_sequence")
	private String id;

	@Field("name")
	private String name;
	@Field("mobile")
	private String mobile;
	@Field("email")
	private String email;
	@Field("is_verified")
	private boolean isVerified;

	@Field("created_at")
	@CreatedDate
	private LocalDateTime createdAt;

	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;

	@Field("restaurant_id")
	private String restaurantId;
}
