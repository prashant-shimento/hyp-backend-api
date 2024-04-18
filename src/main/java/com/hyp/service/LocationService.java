package com.hyp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.hyp.entity.Payment;
import com.hyp.model.PlaceData;
import com.hyp.model.PlacePredictionData;
import com.hyp.request.PredictionRequest;
import reactor.core.publisher.Mono;

@Component
public class LocationService extends BaseServiceImpl<Payment, String> {

	@Value("${google.api.key}")
	private String googleApiKey;

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

	public PlaceData getPlace(String placeId) {
		try {
			WebClient webClient = WebClient.builder().baseUrl("https://places.googleapis.com/v1/places/" + placeId)
					.defaultHeader("X-Goog-Api-Key", googleApiKey)
					.defaultHeaders(headers -> headers.set("X-Goog-FieldMask", "id,formattedAddress,location")).build();
			Mono<PlaceData> placeResponse = webClient.get().retrieve().bodyToMono(PlaceData.class);
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

}
