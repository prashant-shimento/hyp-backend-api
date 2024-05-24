package com.hyp.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.hyp.constants.Constants;
import com.hyp.constants.Constants.ApiStatus;
import com.hyp.entity.ApiLog;
import com.hyp.model.PlacePredictionData;
import com.hyp.request.PredictionRequest;

import reactor.core.publisher.Mono;

@Component
public class LocationService {

	@Value("${google.api.key}")
	private String googleApiKey;

	@Autowired
	ApiRequestResponseLogService apiRequestResponseLogService;

	public boolean isLocationDeliverable(double userLatitude, double userLongitude, double restaurantLatitude,
			double restaurantLongitude, double radius) {
		double distance = calculateDistance(userLatitude, userLongitude, restaurantLatitude, restaurantLongitude);
		return distance <= radius;
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
			Instant requestTime = Instant.now();
			PredictionRequest predictionRequest = new PredictionRequest();
			predictionRequest.setInput(search);
			List<String> region = new ArrayList<>();
			region.add("in");
			predictionRequest.setIncludedRegionCodes(region);
			String apiUrl = "https://places.googleapis.com/v1/places:autocomplete";

			WebClient webClient = WebClient.builder().baseUrl(apiUrl).defaultHeader("X-Goog-Api-Key", googleApiKey)
					.build();
			Mono<PlacePredictionData> placePredictionResponse = webClient.post()
					.body(BodyInserters.fromValue(predictionRequest)).retrieve().bodyToMono(PlacePredictionData.class);
			placePredictionResponse.subscribe(response -> {
				Instant responseTime = Instant.now();
				try {
					apiRequestResponseLogService.save(new ApiLog("Get Prediction Location API", apiUrl,
							Constants.OUTBOUND_API_LOG, "GET", new ObjectMapper().writeValueAsString(predictionRequest),
							new ObjectMapper().writeValueAsString(response), requestTime, responseTime,
							Duration.between(requestTime, responseTime), ApiStatus.SUCCESSFUL));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
				Instant responseTime = Instant.now();

				try {
					apiRequestResponseLogService.save(new ApiLog("Get Prediction Location API", apiUrl,
							Constants.OUTBOUND_API_LOG, "GET", new ObjectMapper().writeValueAsString(predictionRequest),
							error.getMessage(), requestTime, responseTime, Duration.between(requestTime, responseTime),
							ApiStatus.FAILURE));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			});
			return placePredictionResponse.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getLocationPrediction: " + e.getMessage(), e);
		}
	}

	public String getPlaceDetails(String placeId) {
		try {
			Instant requestTime = Instant.now();

			String url = "https://places.googleapis.com/v1/places/" + placeId;
			WebClient webClient = WebClient.builder().baseUrl(url)
					.defaultHeader("X-Goog-Api-Key", googleApiKey)
					.defaultHeaders(headers -> headers.set("X-Goog-FieldMask", "*")).build();
			Mono<String> placeResponse = webClient.get().retrieve().bodyToMono(String.class);
			placeResponse.subscribe(response -> {
				Instant responseTime = Instant.now();
				apiRequestResponseLogService.save(new ApiLog("Get Place Details API", url, Constants.OUTBOUND_API_LOG,
						"GET", "", response, requestTime, responseTime, Duration.between(requestTime, responseTime),
						ApiStatus.SUCCESSFUL));
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
				Instant responseTime = Instant.now();
				apiRequestResponseLogService.save(new ApiLog("Get Place Details API", url, Constants.OUTBOUND_API_LOG,
						"GET", "", error.getMessage(), requestTime, responseTime,
						Duration.between(requestTime, responseTime), ApiStatus.FAILURE));
			});
			return placeResponse.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getPlaceDetails: " + e.getMessage(), e);
		}
	}

	public String getPlaceByGeocodeByRest(double latitude, double longitude) {
		try {
			String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude
					+ "&key=" + googleApiKey;
			Instant requestTime = Instant.now();
			WebClient webClient = WebClient.builder().baseUrl(url).build();
			Mono<String> placeResponse = webClient.get().retrieve().bodyToMono(String.class);
			placeResponse.subscribe(response -> {
				Instant responseTime = Instant.now();
				apiRequestResponseLogService.save(
						new ApiLog("Geocoding API", url, Constants.OUTBOUND_API_LOG, "GET", "", response, requestTime,
								responseTime, Duration.between(requestTime, responseTime), ApiStatus.SUCCESSFUL));
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
				Instant responseTime = Instant.now();
				apiRequestResponseLogService.save(new ApiLog("Geocoding API", url, Constants.OUTBOUND_API_LOG, "GET",
						"", error.getMessage(), requestTime, responseTime, Duration.between(requestTime, responseTime),
						ApiStatus.FAILURE));
			});
			return placeResponse.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getPlaceByGeocodeByRest: " + e.getMessage(), e);
		}
	}

	public GeocodingResult[] getPlaceByGeocodebyClient(double latitude, double longitude) {
		try {
			Instant requestTime = Instant.now();
			GeoApiContext context = new GeoApiContext.Builder().apiKey(googleApiKey).build();
			LatLng latLng = new LatLng(latitude, longitude);
			GeocodingResult[] results = GeocodingApi.reverseGeocode(context, latLng).await();
			Instant responseTime = Instant.now();
			apiRequestResponseLogService.save(new ApiLog("Get place by Geocoding AP", "", Constants.INBOUND_API_LOG, "GET", "",
					results.toString(), requestTime, responseTime, Duration.between(requestTime, responseTime),
					ApiStatus.SUCCESSFUL));

			return results;
		} catch (Exception e) {
			e.printStackTrace();
			Instant requestTime = Instant.now();
			Instant responseTime = Instant.now();
			apiRequestResponseLogService
					.save(new ApiLog("Get Geocoding API", "", Constants.INBOUND_API_LOG, "GET", "", e.getMessage(),
							requestTime, responseTime, Duration.between(requestTime, responseTime), ApiStatus.FAILURE));
			throw new RuntimeException("Error in getPlaceByGeocodebyClient: " + e.getMessage(), e);
		}
	}

}
