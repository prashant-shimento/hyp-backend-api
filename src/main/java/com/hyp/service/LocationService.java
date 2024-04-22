package com.hyp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.hyp.model.PlacePredictionData;
import com.hyp.request.PredictionRequest;
import reactor.core.publisher.Mono;

@Component
public class LocationService {

	@Value("${google.api.key}")
	private String googleApiKey;

	public boolean isLocationDeliverable(double userLatitude, double userLongitude, double restaurantLatitude,
			double restaurantLongitude, double radius) {
		double distance = calculateDistance(userLatitude, userLongitude, restaurantLatitude, restaurantLongitude);
		return distance/1000 <= radius;
	}

	private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 6371;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return earthRadius * c;
	}

	public PlacePredictionData getLocationPrediction(String search) {
		try {
			PredictionRequest predictionRequest = new PredictionRequest();
			predictionRequest.setInput(search);
			List<String> region = new ArrayList<>();
			region.add("in");
			predictionRequest.setIncludedRegionCodes(region);

			WebClient webClient = WebClient.builder().baseUrl("https://places.googleapis.com/v1/places:autocomplete")
					.defaultHeader("X-Goog-Api-Key", googleApiKey).build();
			Mono<PlacePredictionData> placePredictionResponse = webClient.post()
					.body(BodyInserters.fromValue(predictionRequest)).retrieve().bodyToMono(PlacePredictionData.class);
			placePredictionResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return placePredictionResponse.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getLocationPrediction: " + e.getMessage(), e);
		}
	}

	public String getPlaceDetails(String placeId) {
		try {
			WebClient webClient = WebClient.builder().baseUrl("https://places.googleapis.com/v1/places/" + placeId)
					.defaultHeader("X-Goog-Api-Key", googleApiKey)
					.defaultHeaders(headers -> headers.set("X-Goog-FieldMask", "*")).build();
			Mono<String> placeResponse = webClient.get().retrieve().bodyToMono(String.class);
			placeResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return placeResponse.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getLocationPrediction: " + e.getMessage(), e);
		}
	}

	public String getPlaceByGeocodeByRest(double latitude, double longitude) {
		try {
			String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude
					+ "&key=" + googleApiKey;
			WebClient webClient = WebClient.builder().baseUrl(url).build();
			Mono<String> placeResponse = webClient.get().retrieve().bodyToMono(String.class);
			placeResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return placeResponse.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getPlaceByGeocode: " + e.getMessage(), e);
		}
	}

	public GeocodingResult[] getPlaceByGeocodebyClient(double latitude, double longitude) {
		try {
			GeoApiContext context = new GeoApiContext.Builder().apiKey(googleApiKey).build();
			LatLng latLng = new LatLng(latitude, longitude);
			GeocodingResult[] results = GeocodingApi.reverseGeocode(context, latLng).await();
			return results;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getPlaceByGeocodebyClient: " + e.getMessage(), e);
		}
	}

}
