package com.hyp.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReportDto {

	private String id;
	private String name;
	private String description;
	private String collectionName;
	private List<Map<String, Object>> parameters;
	private List<Map<String, Object>> query;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

}
