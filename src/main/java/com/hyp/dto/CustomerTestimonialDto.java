package com.hyp.dto;

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
public class CustomerTestimonialDto {

	private String partnerId;
	private String restaurantId;
	private String customerName;
	private String reviews;
	private Double rating;
}
