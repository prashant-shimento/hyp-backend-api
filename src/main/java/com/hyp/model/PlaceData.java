package com.hyp.model;

import lombok.Data;

@Data
public class PlaceData {

	private String id;
	private String formattedAddress;
	private Location location;

	@Data
	public static class Location {
		private double latitude;
		private double longitude;
	}

}
