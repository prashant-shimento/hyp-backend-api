package com.hyp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.hyp.entity.Delivery;
import com.hyp.model.DeliveryQuote;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;

import reactor.core.publisher.Mono;

@Service
public class DeliveryService extends BaseServiceImpl<Delivery, String> {

	@Value("${delivery.pidge.url}")
	private String baseUrl;

	private static final double DELIVERY_RADIUS_KM = 5.0;

	public boolean isLocationDeliverable(double userLatitude, double userLongitude, double restaurantLatitude,
			double restaurantLongitude) {
		double distance = calculateDistance(userLatitude, userLongitude, restaurantLatitude, restaurantLongitude);
		return distance <= DELIVERY_RADIUS_KM;
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

	public String createDeliveryOrder(DeliveryOrderRequest deliveryOrderRequest) {
		try {
			System.out.println("Request " + new ObjectMapper().writeValueAsString(deliveryOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjphZmEyYjlmMC1mMzI5LTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			String endpoint = "/v1.0/store/channel/vendor/order";
			Mono<String> createOrderResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryOrderRequest)).retrieve().bodyToMono(String.class);
			String response = createOrderResponse.block();
			if (response == null) {
				throw new RuntimeException("No response received from the server.");
			}

			JsonNode jsonResponse = new ObjectMapper().readTree(response);
			JsonNode dataNode = jsonResponse.get("data");
			if (dataNode == null) {
				throw new RuntimeException("Response does not contain 'data' field.");
			}

			System.out.println("Data Node response: " + dataNode.toString());
			JsonNode orderIdNode = dataNode.get(deliveryOrderRequest.getTrips().get(0).getSourceOrderId());
			if (orderIdNode == null) {
				throw new RuntimeException("Response does not contain order ID for the specified source order.");
			}

			String orderId = orderIdNode.asText();
			System.out.println("Order Id: " + orderId);
			return orderId;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new RuntimeException("Error processing JSON response.", e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error occurred while creating delivery order.", e);
		}
	}

	public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) {
		try {
			System.out.println("Request " + new ObjectMapper().writeValueAsString(deliveryQuoteRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjphZmEyYjlmMC1mMzI5LTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			String endpoint = "/v1.0/store/channel/vendor/quote";
			Mono<DeliveryQuote> quoteResponseMono = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryQuoteRequest)).retrieve().bodyToMono(DeliveryQuote.class);
			quoteResponseMono.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return quoteResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getDeliveryQuote: " + e.getMessage(), e);
		}
	}

}
