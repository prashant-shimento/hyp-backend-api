package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;
import com.hyp.repository.DeliveryRepository;
import com.hyp.request.DeliveryFulfillRequest;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DeliveryService extends BaseServiceImpl<Delivery, String> {

	@Value("${delivery.pidge.url}")
	private String baseUrl;

	@Value("${delivery.pidge.token}")
	private String token;

	@Value("${delivery.pidge.smart.id}")
	private String smartId;

	@Autowired
	DeliveryRepository deliveryRepository;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	ApiLogService apiRequestResponseLogService;

	@Autowired
	RetryTemplate retryTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	PosService posService;

	@Autowired
	OrderService orderService;

	public Delivery findByOrderId(String orderId) {
		return deliveryRepository.findByOrderId(orderId);
	}

	public Delivery findByDeliveryOrderId(String deliveryOrderId) {
		return deliveryRepository.findByDeliveryOrderId(deliveryOrderId);
	}

	@Retryable(retryFor = { Exception.class })
	public String createDeliveryOrder(DeliveryOrderRequest deliveryOrderRequest) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order";
		try {
			log.info("createDeliveryOrder Request {}", objectMapper.writeValueAsString(deliveryOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, token)
					.build();
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

			JsonNode orderIdNode = dataNode.get(deliveryOrderRequest.getTrips().get(0).getSourceOrderId());
			if (orderIdNode == null) {
				throw new DeliveryException("Response does not contain order ID for the specified source order.");
			}
			log.info("createDeliveryOrder Response {}", objectMapper.writeValueAsString(response));
			String orderId = orderIdNode.asText();
			return orderId;
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error processing JSON response " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class })
	public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) throws DeliveryException {
		try {

			log.info("getDeliveryQuote Request {}", objectMapper.writeValueAsString(deliveryQuoteRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, token)
					.build();
			String endpoint = "/v1.0/store/channel/vendor/quote";
			Mono<DeliveryQuote> quoteResponseMono = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryQuoteRequest)).retrieve().bodyToMono(DeliveryQuote.class);
			quoteResponseMono.subscribe(response -> {
			}, error -> {
				log.error("Error response: " + error.getMessage());
			});
			log.info("getDeliveryQuote Response {}", objectMapper.writeValueAsString(quoteResponseMono.block()));
			return quoteResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error in getDeliveryQuote: " + e.getMessage());
		}
	}
	
	@Retryable(retryFor = { Exception.class })
	public DeliveryQuote getServiceability(String deliveryOrderId) throws DeliveryException {
		try {

			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, token)
					.build();
			String endpoint = "v1.0/store/channel/vendor/order/fulfillment/services?ids=" + deliveryOrderId;
			log.info("getServiceability endPoint {}", endpoint);
			Mono<DeliveryQuote> quoteResponseMono = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryQuote.class);
			quoteResponseMono.subscribe(response -> {
				log.info("Response: " + response);

			}, error -> {
				log.error("Error response: " + error.getMessage());
			});
			log.info("getServiceability Response {}", objectMapper.writeValueAsString(quoteResponseMono));
			return quoteResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error in getDeliveryQuote: " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class })
	public void initiateOrderFulfill(DeliveryFulfillRequest deliveryFulfillRequest) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order/fulfill";
		try {
			log.info("initiateOrderFulfill Request {}", objectMapper.writeValueAsString(deliveryFulfillRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, token)
					.build();
			Mono<Object> responses = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryFulfillRequest)).exchangeToMono(response -> {
						HttpStatus statusCode = (HttpStatus) response.statusCode();
						log.info("Status code: " + statusCode);
						return Mono.just(statusCode);
					});
			log.info("initiateOrderFulfill Response {}", objectMapper.writeValueAsString(responses));
			responses.subscribe();
		} catch (Exception e) {
			e.printStackTrace();
			String errorMessage = "Error in initiateOrderFulfill: " + e.getMessage();
			throw new DeliveryException(errorMessage);
		}
	}

	public DeliveryRiderLocation getRiderCurrentLocation(String deliveryOrderId) {
		String endpoint = "/v1.0/store/channel/vendor/order/" + deliveryOrderId + "/fulfillment/tracking";
		try {
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, token)
					.build();
			log.info("getRiderCurrentLocation endPoint {}", endpoint);
			Mono<DeliveryRiderLocation> deliveryLocationResponseMono = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryRiderLocation.class);
			deliveryLocationResponseMono.subscribe(response -> {
				log.info("Response: " + response);

			}, error -> {
				log.error("Error response: " + error.getMessage());
			});
			log.info("getRiderCurrentLocation Response {}",
					objectMapper.writeValueAsString(deliveryLocationResponseMono));
			return deliveryLocationResponseMono.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in getDeliveryQuote: " + e.getMessage(), e);
		}
	}

	public void cancelDeliveryOrder(String deliveryOrderId) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/" + deliveryOrderId + "/cancel";
		try {
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, token)
					.build();
			Mono<Object> responses = webClient.post().uri(endpoint).exchangeToMono(response -> {
				HttpStatus statusCode = (HttpStatus) response.statusCode();
				log.info("Status code: " + statusCode);
				return Mono.just(statusCode);
			});
			log.info("cancelDeliveryOrder Response {}", objectMapper.writeValueAsString(responses));
			responses.subscribe();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error in cancelDeliveryOrder: " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class })
	public void initiateSmartFulfill(DeliveryFulfillRequest deliveryFulfillRequest) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order/fulfill/smart";
		try {
			deliveryFulfillRequest.setSmartId(smartId);
			log.info("initiateSmartFulfill Request {}", objectMapper.writeValueAsString(deliveryFulfillRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, token)
					.build();
			Mono<Object> responses = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryFulfillRequest)).exchangeToMono(response -> {
						HttpStatus statusCode = (HttpStatus) response.statusCode();
						log.info("Status code: " + statusCode);
						return Mono.just(statusCode);
					});
			log.info("initiateSmartFulfill Response {}", objectMapper.writeValueAsString(responses));
			responses.subscribe();
		} catch (Exception e) {
			e.printStackTrace();
			String errorMessage = "Error in initiateSmartFulfill: " + e.getMessage();
			throw new DeliveryException(errorMessage);
		}
	}

	@Retryable(retryFor = { Exception.class })
	public DeliveryOrderStatus getDeliveryStatus(String deliveryOrderId) throws DeliveryException {
		try {

			WebClient webClient = WebClient.builder().baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, token)
					.build();
			String endpoint = "v1.0/store/channel/vendor/order/" + deliveryOrderId;
			log.info("getDeliveryStatus endPoint {}", endpoint);
			Mono<DeliveryOrderStatus> deliveryOrderStatusResponse = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryOrderStatus.class);
			deliveryOrderStatusResponse.subscribe(response -> {
				log.info("Response: " + response);

			}, error -> {
				log.error("Error response: " + error.getMessage());
			});
			log.info("getDeliveryStatus Response {}", objectMapper.writeValueAsString(deliveryOrderStatusResponse));
			return deliveryOrderStatusResponse.block();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error in getDeliveryStatus: " + e.getMessage());
		}
	}

	public void processDeliveryCallback(Delivery delivery, DeliveryOrderStatus deliveryOrderData)
			throws DeliveryException {
		try {
			delivery.setStatus(DeliveryOrderStatusType.getDeliveryOrderStatus(deliveryOrderData.getStatus()));
			if (delivery.getStatus() == DeliveryOrderStatusType.FULFILLED
					|| delivery.getStatus() == DeliveryOrderStatusType.COMPLETED) {
				DeliveryFulfillment deliveryFulfill = deliveryOrderData.getFulfillment();
				DeliveryFulfillStatusType fullFillStatus = deliveryFulfill.getStatus();
				delivery.setFulfillment(deliveryFulfill);
				Order order = orderService.findById(delivery.getOrderId());
				orderService.updateOrderStatus(order.getId(),
						OrderStatusType.getOrderStatusByDelvieryStatus(fullFillStatus));
				if (deliveryOrderData.getFulfillment().getTrackCode() != null
						&& order.getDeliveryTrackingLink() == null) {
					delivery.getFulfillment().setTrackCode(deliveryOrderData.getFulfillment().getTrackCode());
					order.setDeliveryTrackingLink("https://t.pidge.in?t=" + delivery.getFulfillment().getTrackCode());
					orderService.save(order);
				}
				if (posService.isPosUpdateRequired(fullFillStatus)) {
					posService.updatePosRiderStatus(delivery, order);
				}
				this.save(delivery);
			}
		} catch (Exception e) {
			throw new DeliveryException("Error occurred in processDeliveryCallback " + e.getMessage());
		}
	}
}
