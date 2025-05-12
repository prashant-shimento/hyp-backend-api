package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderTypeDto {
	private String id;
	private String orderType;
	private int orderTypeId;
	private String createdAt;
	private String updatedAt;
	private String restaurantId;
	private boolean isDeleted;
}
