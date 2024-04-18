package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.RiderStatusType;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.repository.DeliveryRepository;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.request.DeliveryFulfillRequest;

import reactor.core.publisher.Mono;

@Service
public class DeliveryService extends BaseServiceImpl<Delivery, String> {

	@Value("${delivery.pidge.url}")
	private String baseUrl;

	@Autowired
	DeliveryRepository deliveryRepository;

	@Autowired
	PosService posService;

	@Autowired
	RestaurantService restaurantService;
	
	@Autowired
	RetryTemplate retryTemplate;

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

	public boolean isPosUpdateRequired(DeliveryFulfillStatusType fullFillStatus) {
		return fullFillStatus == DeliveryFulfillStatusType.CREATED
				|| fullFillStatus == DeliveryFulfillStatusType.OUT_FOR_PICKUP
				|| fullFillStatus == DeliveryFulfillStatusType.REACHED_PICKUP
				|| fullFillStatus == DeliveryFulfillStatusType.PICKED_UP
				|| fullFillStatus == DeliveryFulfillStatusType.DELIVERED;
	}

	public String createDeliveryOrder(DeliveryOrderRequest deliveryOrderRequest) throws DeliveryException {
		try {
			System.out.println("Request " + new ObjectMapper().writeValueAsString(deliveryOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			String endpoint = "/v1.0/store/channel/vendor/order";
			Mono<String> createOrderResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryOrderRequest)).retrieve().bodyToMono(String.class);
			String response = createOrderResponse.block();
			if (response == null) {
				throw new DeliveryException("No response received from the server.");
			}

			JsonNode jsonResponse = new ObjectMapper().readTree(response);
			JsonNode dataNode = jsonResponse.get("data");
			if (dataNode == null) {
				throw new DeliveryException("Response does not contain 'data' field.");
			}

			System.out.println("Data Node response: " + dataNode.toString());
			JsonNode orderIdNode = dataNode.get(deliveryOrderRequest.getTrips().get(0).getSourceOrderId());
			if (orderIdNode == null) {
				throw new DeliveryException("Response does not contain order ID for the specified source order.");
			}

			String orderId = orderIdNode.asText();
			System.out.println("Order Id: " + orderId);
			return orderId;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new DeliveryException("Error processing JSON response "+ e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error occurred while creating delivery order "+ e.getMessage());
		}
	}

    @Retryable(retryFor = {Exception.class})
	public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) throws DeliveryException {
		try {
			System.out.println("Request " + new ObjectMapper().writeValueAsString(deliveryQuoteRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
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
			throw new DeliveryException("Error in getDeliveryQuote: " + e.getMessage());
		}
	}

	public DeliveryQuote getServiceability(String deliveryOrderId) throws DeliveryException {
		try {
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			String endpoint = "v1.0/store/channel/vendor/order/fulfillment/services?ids=" + deliveryOrderId;
			System.out.println("Endpoint : " + endpoint);
			Mono<DeliveryQuote> quoteResponseMono = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryQuote.class);
			quoteResponseMono.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return quoteResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error in getDeliveryQuote: " + e.getMessage());
		}
	}

	public boolean initiateOrderFulfill(DeliveryFulfillRequest deliveryFulfillRequest) throws DeliveryException {
		try {
			System.out.println("Request " + new ObjectMapper().writeValueAsString(deliveryFulfillRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			String endpoint = "/v1.0/store/channel/vendor/order/fulfill";
			ClientResponse response = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryFulfillRequest)).retrieve().bodyToMono(ClientResponse.class)
					.block();
			System.out.println("response from fulfill " + response + " " + response.statusCode());
			return response.statusCode().equals(HttpStatus.OK) ? true : false;

		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error in initiateOrderFulfill: " + e.getMessage());
		}
	}

	public DeliveryRiderLocation getRiderCurrentLocation(String deliveryOrderId) {
		try {
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			String endpoint = "/v1.0/store/channel/vendor/order/" + deliveryOrderId + "/fulfillment/tracking";
			System.out.println("Endpoint : " + endpoint);
			Mono<DeliveryRiderLocation> deliveryLocationResponseMono = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryRiderLocation.class);
			deliveryLocationResponseMono.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return deliveryLocationResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getDeliveryQuote: " + e.getMessage(), e);
		}
	}

	public Delivery findByOrderId(String orderId) {
		return deliveryRepository.findByOrderId(orderId);
	}

	public Delivery findByDeliveryOrderId(String deliveryOrderId) {
		return deliveryRepository.findByDeliveryOrderId(deliveryOrderId);
	}

	public void updatePosRiderStatus(Delivery delivery,Order order) {
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		RiderDetails riderDetails = new RiderDetails(delivery.getFulfillment().getRider().getName(),
				delivery.getFulfillment().getRider().getMobile());

		RiderStatusType riderStatus = RiderStatusType
				.getRiderStatusByDeliveryRider(delivery.getFulfillment().getStatus());

		PosRiderUpdateRequest posRiderUpdateRequest = PosOrderRequestTranslation
				.getPosRiderStatusUpdateRequest(restaurant, order, riderDetails, riderStatus);

		posService.updatePosRiderStatus(posRiderUpdateRequest);
	}
}
