package com.hyp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
	private String address;
	private String addressType;
	private double latitude;
	private double longitude;


}
