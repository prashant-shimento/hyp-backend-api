package com.hyp.request;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeliveryQuoteRequest {
	private Pickup pickup;
	private List<Drop> drop;

	@Data
	@NoArgsConstructor
	public static class Pickup {
		private Coordinates coordinates;
		private String pincode;
	}

	@Data
	@NoArgsConstructor
	public static class Drop {
		private String ref;
		private Location location;
	}

	@Data
	@NoArgsConstructor
	public static class Location {
		private Coordinates coordinates;
		private String pincode;
	}

	@Data
	@NoArgsConstructor
	public static class Coordinates {
		private double latitude;
		private double longitude;
	}
}
