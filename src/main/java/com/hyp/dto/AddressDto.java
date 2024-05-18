package com.hyp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto extends BaseDto {

	@NotNull(message = "Customer ID is mandatory")
	private String customerId;

	@NotBlank(message = "Address type is mandatory")
	private String addressType;

	@NotBlank(message = "Address line one is mandatory")
	private String addressOne;

	private String addressTwo;

	private String landmark;

	@NotBlank(message = "City is mandatory")
	private String city;

	@NotBlank(message = "State is mandatory")
	private String state;

	@NotBlank(message = "Country is mandatory")
	private String country;

	@NotBlank(message = "Pincode is mandatory")
	private String pincode;

	@NotNull(message = "Latitude is mandatory")
	private Double latitude;

	@NotNull(message = "Longitude is mandatory")
	private Double longitude;

}
