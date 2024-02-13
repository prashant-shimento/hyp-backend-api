package com.hyp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RestaurantDto {
	private String restaurantId;
	private String active;
	private DetailsDto details;
}
