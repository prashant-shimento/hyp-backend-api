package com.hyp.entity;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.persistence.Id;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.annotation.GenerateId;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reports")
public class Report {
	
	@Id
	@GenerateId()
	private String id;
	
	private String name;
	private String description;
	private String collectionName;
	private List<Map<String, Object>> parameters;
	private List<Map<String, Object>> query;
	
	@Field("created_at")
	@CreatedDate
	private LocalDateTime createdAt = LocalDateTime.now();;

	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;

}
