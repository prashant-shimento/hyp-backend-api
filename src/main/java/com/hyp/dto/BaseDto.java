package com.hyp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class BaseDto {

	private String id;
	private String createdAt;
	private String updatedAt;
	private String restaurantId;
}
