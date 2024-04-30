package com.hyp.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.hyp.constants.Constants;
import com.hyp.constants.Constants.ApiStatus;
import com.hyp.entity.ApiLog;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.RiderStatusType;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.repository.DeliveryRepository;
import com.hyp.request.DeliveryFulfillRequest;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.translation.PosOrderRequestTranslation;

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
	ApiRequestResponseLogService apiRequestResponseLogService;

	@Autowired
	RetryTemplate retryTemplate;

	public boolean isPosUpdateRequired(DeliveryFulfillStatusType fullFillStatus) {
		return fullFillStatus == DeliveryFulfillStatusType.OUT_FOR_PICKUP
				|| fullFillStatus == DeliveryFulfillStatusType.REACHED_PICKUP
				|| fullFillStatus == DeliveryFulfillStatusType.PICKED_UP
				|| fullFillStatus == DeliveryFulfillStatusType.DELIVERED;
	}

	public String createDeliveryOrder(DeliveryOrderRequest deliveryOrderRequest) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order";
		try {
			Instant requestTime = Instant.now();

			System.out.println("Request " + new ObjectMapper().writeValueAsString(deliveryOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
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
			Instant responseTime = Instant.now();

			apiRequestResponseLogService
					.save(new ApiLog("Create Delivery Order API", baseUrl + endpoint, Constants.OUTBOUND_API_LOG,
							"POST", new ObjectMapper().writeValueAsString(deliveryOrderRequest), response, requestTime,
							responseTime, Duration.between(requestTime, responseTime), ApiStatus.SUCCESSFUL));

			return orderId;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new DeliveryException("Error processing JSON response " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			Instant requestTime = Instant.now();
			Instant responseTime = Instant.now();
			String errorMessage = "Error occurred while creating delivery order " + e.getMessage();
			apiRequestResponseLogService.save(new ApiLog("Create Delivery Order API", baseUrl + endpoint,
					Constants.OUTBOUND_API_LOG, "POST", deliveryOrderRequest.toString(), errorMessage, requestTime,
					responseTime, Duration.between(requestTime, responseTime), ApiStatus.FAILURE));
			throw new DeliveryException(errorMessage);
		}
	}

	@Retryable(retryFor = { Exception.class })
	public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) throws DeliveryException {
		try {
			Instant requestTime = Instant.now();

			System.out.println("Request " + new ObjectMapper().writeValueAsString(deliveryQuoteRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			String endpoint = "/v1.0/store/channel/vendor/quote";
			Mono<DeliveryQuote> quoteResponseMono = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryQuoteRequest)).retrieve().bodyToMono(DeliveryQuote.class);
			quoteResponseMono.subscribe(response -> {
				System.out.println("Response: " + response);
				Instant responseTime = Instant.now();
				try {
					apiRequestResponseLogService
							.save(new ApiLog("Get Delivery Quote API", endpoint + baseUrl, Constants.OUTBOUND_API_LOG,
									"GET", new ObjectMapper().writeValueAsString(deliveryQuoteRequest),
									new ObjectMapper().writeValueAsString(response), requestTime, responseTime,
									Duration.between(requestTime, responseTime), ApiStatus.SUCCESSFUL));

				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}

			}, error -> {
				System.err.println("Error response: " + error.getMessage());
				Instant responseTime = Instant.now();
				String errorMessage = "Error in getDeliveryQuote: " + error.getMessage();
				apiRequestResponseLogService.save(new ApiLog("Get Delivery Quote API", endpoint + baseUrl,
						Constants.OUTBOUND_API_LOG, "GET", deliveryQuoteRequest.toString(), errorMessage, requestTime,
						responseTime, Duration.between(requestTime, responseTime), ApiStatus.FAILURE));
			});
			return quoteResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error in getDeliveryQuote: " + e.getMessage());
		}
	}

	public DeliveryQuote getServiceability(String deliveryOrderId) throws DeliveryException {
		try {
			Instant requestTime = Instant.now();

			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			String endpoint = "v1.0/store/channel/vendor/order/fulfillment/services?ids=" + deliveryOrderId;
			System.out.println("Endpoint : " + endpoint);
			Mono<DeliveryQuote> quoteResponseMono = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryQuote.class);
			quoteResponseMono.subscribe(response -> {
				System.out.println("Response: " + response);
				Instant responseTime = Instant.now();
				try {
					apiRequestResponseLogService
							.save(new ApiLog("Get Service Ability API", baseUrl + endpoint, Constants.OUTBOUND_API_LOG,
									"GET", "", new ObjectMapper().writeValueAsString(response), requestTime,
									responseTime, Duration.between(requestTime, responseTime), ApiStatus.SUCCESSFUL));
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
				Instant responseTime = Instant.now();
				apiRequestResponseLogService.save(new ApiLog("Get Service Ability API", baseUrl + endpoint,
						Constants.OUTBOUND_API_LOG, "GET", "", "Error in getServiceability: " + error.getMessage(),
						requestTime, responseTime, Duration.between(requestTime, responseTime), ApiStatus.FAILURE));
			});
			return quoteResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error in getDeliveryQuote: " + e.getMessage());
		}
	}

	public void initiateOrderFulfill(DeliveryFulfillRequest deliveryFulfillRequest) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order/fulfill";

		try {
			Instant requestTime = Instant.now();

			System.out.println("Request " + new ObjectMapper().writeValueAsString(deliveryFulfillRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			Mono<Object> responses = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryFulfillRequest)).exchangeToMono(response -> {
						HttpStatus statusCode = (HttpStatus) response.statusCode();
						// Do something with the status code
						Instant responseTime = Instant.now();

						System.out.println("Status code: " + statusCode);
						try {
							apiRequestResponseLogService.save(new ApiLog("Initiate Order Fulfill API",
									baseUrl + endpoint, Constants.OUTBOUND_API_LOG, "POST",
									new ObjectMapper().writeValueAsString(deliveryFulfillRequest),
									new ObjectMapper().writeValueAsString(response), requestTime, responseTime,
									Duration.between(requestTime, responseTime), ApiStatus.SUCCESSFUL));
						} catch (JsonProcessingException e) {
							e.printStackTrace();
						}
						return Mono.just(statusCode);
					});

			responses.subscribe();
		} catch (Exception e) {
			e.printStackTrace();
			Instant requestTime = Instant.now();
			Instant responseTime = Instant.now();
			String errorMessage = "Error in initiateOrderFulfill: " + e.getMessage();
			try {
				apiRequestResponseLogService.save(new ApiLog("Initiate Order Fulfill API", baseUrl + endpoint,
						Constants.OUTBOUND_API_LOG, "POST",
						new ObjectMapper().writeValueAsString(deliveryFulfillRequest), errorMessage, requestTime,
						responseTime, Duration.between(requestTime, responseTime), ApiStatus.FAILURE));
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			throw new DeliveryException(errorMessage);
		}
	}

	public DeliveryRiderLocation getRiderCurrentLocation(String deliveryOrderId) {
		String endpoint = "/v1.0/store/channel/vendor/order/" + deliveryOrderId + "/fulfillment/tracking";

		try {
			Instant requestTime = Instant.now();

			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			System.out.println("Endpoint : " + endpoint);
			Mono<DeliveryRiderLocation> deliveryLocationResponseMono = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryRiderLocation.class);
			deliveryLocationResponseMono.subscribe(response -> {
				System.out.println("Response: " + response);
				Instant responseTime = Instant.now();
				apiRequestResponseLogService.save(new ApiLog("Get Rider Current Location API", baseUrl + endpoint,
						"GET", Constants.OUTBOUND_API_LOG, "", response.toString(), requestTime, responseTime,
						Duration.between(requestTime, responseTime), ApiStatus.SUCCESSFUL));

			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return deliveryLocationResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			Instant requestTime = Instant.now();
			Instant responseTime = Instant.now();
			apiRequestResponseLogService.save(new ApiLog("Get Rider Current Location API", baseUrl + endpoint, "GET",
					Constants.OUTBOUND_API_LOG, "", e.getMessage(), requestTime, responseTime,
					Duration.between(requestTime, responseTime), ApiStatus.FAILURE));
			throw new RuntimeException("Error in getDeliveryQuote: " + e.getMessage(), e);
		}
	}

	public Delivery findByOrderId(String orderId) {
		return deliveryRepository.findByOrderId(orderId);
	}

	public Delivery findByDeliveryOrderId(String deliveryOrderId) {
		return deliveryRepository.findByDeliveryOrderId(deliveryOrderId);
	}

	public void updatePosRiderStatus(Delivery delivery, Order order) {
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		RiderDetails riderDetails = new RiderDetails(delivery.getFulfillment().getRider().getName(),
				delivery.getFulfillment().getRider().getMobile());

		RiderStatusType riderStatus = RiderStatusType
				.getRiderStatusByDeliveryRider(delivery.getFulfillment().getStatus());

		PosRiderUpdateRequest posRiderUpdateRequest = PosOrderRequestTranslation
				.getPosRiderStatusUpdateRequest(restaurant, order, riderDetails, riderStatus);

		posService.updatePosRiderStatus(posRiderUpdateRequest);
	}

	public void cancelDeliveryOrder(String deliveryOrderId) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/" + deliveryOrderId + "/cancel";

		try {
			Instant requestTime = Instant.now();

			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION,
					"Bearer c3Rrbjo0OjgwMjo1MGMxMDQ4MC1mN2NjLTExZWUtOTJlYi01N2Y1NTA2YzQ0Mjk=").build();
			Mono<Object> responses = webClient.post().uri(endpoint).exchangeToMono(response -> {
				HttpStatus statusCode = (HttpStatus) response.statusCode();
				// Do something with the status code
				System.out.println("Status code: " + statusCode);
				Instant responseTime = Instant.now();

				apiRequestResponseLogService.save(new ApiLog("Cancel Delivery Order API", baseUrl + endpoint, "POST",
						Constants.OUTBOUND_API_LOG, "",
						response.toString(), requestTime, responseTime,
						Duration.between(requestTime, responseTime), ApiStatus.SUCCESSFUL));

				return Mono.just(statusCode);
			});

			responses.subscribe();
		} catch (Exception e) {
			e.printStackTrace();
			Instant requestTime = Instant.now();
			Instant responseTime = Instant.now();
			apiRequestResponseLogService.save(new ApiLog("Cancel Delivery Order API", baseUrl + endpoint, "POST",
					Constants.OUTBOUND_API_LOG, "", e.getMessage(), requestTime, responseTime,
					Duration.between(requestTime, responseTime), ApiStatus.FAILURE));
			throw new DeliveryException("Error in cancelDeliveryOrder: " + e.getMessage());
		}
	}
}
