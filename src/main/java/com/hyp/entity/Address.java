package com.hyp.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "address")
@JsonInclude(Include.NON_NULL)
public class Address extends BaseEntity {

	private static final long serialVersionUID = 1L;
	@Field("address_type")
	private String addressType;

	@Field("address_one")
	private String addressOne;

	@Field("address_two")
	private String addressTwo;

	@Field("landmark")
	private String landmark;

	@Field("city")
	private String city;

	@Field("state")
	private String state;

	@Field("country")
	private String country;

	@Field("pincode")
	private String pincode;
	
	@Field("customer_id")
	private String customerId;

	private Location location;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Location {
		private double latitude;
		private double longitude;
	}
	
	

}
