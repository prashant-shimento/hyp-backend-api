package com.hyp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
	private String addressType;
	private String addressOne;
	private String addressTwo;
	private String landmark;
	private String city;
	private String state;
	private String country;
	private String pincode;
	private double latitude;
	private double longitude;

}
