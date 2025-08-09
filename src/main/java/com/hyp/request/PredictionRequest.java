package com.hyp.request;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PredictionRequest {

	String input;
	List<String> includedRegionCodes;
	LocationBias locationBias;
	String sessionToken;

	@Data
	@Builder
	public static class LocationBiasCircle {
		private LatLng center;
		private int radius;
	}

	@Data
	@Builder
	public static class LocationBias {
		private LocationBiasCircle circle;
	}

	@Data
	@Builder
	public static class LatLng {
		private double latitude;
		private double longitude;
	}
}
