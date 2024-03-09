package com.hyp.entity;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "address")
public class Address extends BaseEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Field("address")
	private String address;
	@Field("addressType")
	private String addressType;
	private Location location;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Location {
		private double latitude;
		private double longitude;
	}
}
