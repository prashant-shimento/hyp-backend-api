package com.hyp.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.hyp.constants.Constants;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;
import com.hyp.model.DeliveryOrderStatusResponse;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryQuote.DeliveryNetworks;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.repository.DeliveryRepository;
import com.hyp.request.DeliveryFulfillRequest;
import com.hyp.request.DeliveryFulfillResponse;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.request.MailNotificationRequest;
import com.hyp.request.PidgeLoginRequest;
import com.hyp.translation.DeliveryRequestTranslation;
import com.hyp.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DeliveryService extends BaseServiceImpl<Delivery, String> {

	@Value("${delivery.pidge.url}")
	private String baseUrl;

	@Value("${delivery.pidge.smart.id}")
	private Integer smartId;

	@Value("${delivery.pidge.username}")
	private String pidgeUsername;

	@Value("${delivery.pidge.password}")
	private String pidgePassword;

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

	@Autowired
	MailService mailService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private String getToken() throws Exception {
		String token = redisTemplate.opsForValue().get("pidgeToken");
		if (token == null) {
			throw new DeliveryException("Token not found in Redis");
		}
		return "Bearer c3Rrbjo0Ojg1MzpjOTMxMzA2MC0xODAyLTExZWYtYmM0My02MzZlYmUzZTlhNjQ=";
	}

	public String refreshToken() throws Exception {
		String newToken = generateToken();
		if (newToken != null && !newToken.isEmpty()) {
			redisTemplate.opsForValue().set("pidgeToken", newToken);
			return newToken;
		} else {
			throw new DeliveryException("Failed to refresh the token: Received empty token");
		}
	}

	public Delivery findByOrderId(String orderId) {
		return deliveryRepository.findByOrderId(orderId);
	}

	public Delivery findByDeliveryOrderId(String deliveryOrderId) {
		return deliveryRepository.findByDeliveryOrderId(deliveryOrderId);
	}

	public void createOrder(DeliveryOrderRequest deliveryOrderRequest, Order order) throws DeliveryException {
		String deliveryOrderId = createDeliveryOrder(deliveryOrderRequest);
		Delivery delivery = DeliveryRequestTranslation.getDeliveryEntity(deliveryOrderRequest);
		delivery.setId(CommonUtils.genId());
		delivery.setDeliveryOrderId(deliveryOrderId);
		delivery.setStatus(DeliveryOrderStatusType.PENDING);
		delivery.setService(order.getDeliveryDetails().getService());
		delivery.setNetworkId(order.getDeliveryDetails().getNetworkId());
		delivery.setPickupNow(order.getDeliveryDetails().isPickupNow());
		save(delivery);
	}

	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public String createDeliveryOrder(DeliveryOrderRequest deliveryOrderRequest) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order";
		try {
			log.info("createDeliveryOrder Request {}", objectMapper.writeValueAsString(deliveryOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
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
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
			throw new RuntimeException("Operation failed due to unauthorized access. Token refreshed.", e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DeliveryException("Error processing JSON response " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) throws DeliveryException {
		try {

			log.info("getDeliveryQuote Request {}", objectMapper.writeValueAsString(deliveryQuoteRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
			String endpoint = "/v1.0/store/channel/vendor/quote";
			Mono<DeliveryQuote> quoteResponseMono = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryQuoteRequest)).retrieve().bodyToMono(DeliveryQuote.class);
			log.info("getDeliveryQuote Response {}", objectMapper.writeValueAsString(quoteResponseMono.block()));
			return quoteResponseMono.block();
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
			throw new RuntimeException("Operation failed due to unauthorized access. Token refreshed.", e);
		} catch (Exception e) {
			log.error("Error in getDeliveryQuote: {}", e);
			throw new DeliveryException("Error in getDeliveryQuote: " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public DeliveryQuote getServiceability(String deliveryOrderId) throws DeliveryException {
		try {

			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
			String endpoint = "v1.0/store/channel/vendor/order/fulfillment/services?ids=" + deliveryOrderId;
			log.info("getServiceability endPoint {}", endpoint);
			Mono<DeliveryQuote> quoteResponseMono = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryQuote.class);
			log.info("getServiceability Response {}", objectMapper.writeValueAsString(quoteResponseMono));
			return quoteResponseMono.block();
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
			throw new RuntimeException("Operation failed due to unauthorized access. Token refreshed.", e);
		} catch (Exception e) {
			log.error("Error in getServiceability: {}", e);
			throw new DeliveryException("Error in getServiceability: " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public void initiateOrderFulfill(DeliveryFulfillRequest deliveryFulfillRequest) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order/fulfill";
		try {
			log.info("initiateOrderFulfill Request {}", objectMapper.writeValueAsString(deliveryFulfillRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
			Mono<Object> responses = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryFulfillRequest)).exchangeToMono(response -> {
						HttpStatus statusCode = (HttpStatus) response.statusCode();
						log.info("Status code: " + statusCode);
						return Mono.just(statusCode);
					});
			log.info("initiateOrderFulfill Response {}", objectMapper.writeValueAsString(responses));
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
			throw new RuntimeException("Operation failed due to unauthorized access. Token refreshed.", e);
		} catch (Exception e) {
			log.error("Error in initiateOrderFulfill: {}", e);
			throw new DeliveryException("Error in initiateOrderFulfill: " + e.getMessage());
		}
	}
	
	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public DeliveryRiderLocation getRiderCurrentLocation(String deliveryOrderId) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order/" + deliveryOrderId + "/fulfillment/tracking";
		try {
			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
			log.info("getRiderCurrentLocation endPoint {}", endpoint);
			Mono<DeliveryRiderLocation> deliveryLocationResponseMono = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryRiderLocation.class);
			log.info("getRiderCurrentLocation Response {}",
					objectMapper.writeValueAsString(deliveryLocationResponseMono));
			return deliveryLocationResponseMono.block();
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
			throw new RuntimeException("Operation failed due to unauthorized access. Token refreshed.", e);
		} catch (Exception e) {
			log.error("Error in getRiderCurrentLocation: {}", e);
			throw new DeliveryException("Error in getRiderCurrentLocation: " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public void cancelDeliveryOrder(String deliveryOrderId) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/" + deliveryOrderId + "/cancel";
		try {
			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
			Mono<Object> responses = webClient.post().uri(endpoint).exchangeToMono(response -> {
				HttpStatus statusCode = (HttpStatus) response.statusCode();
				log.info("Status code: " + statusCode);
				return Mono.just(statusCode);
			});
			log.info("cancelDeliveryOrder Response {}", objectMapper.writeValueAsString(responses));
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
			throw new RuntimeException("Operation failed due to unauthorized access. Token refreshed.", e);
		} catch (Exception e) {
			log.error("Error in cancelDeliveryOrder: {}", e);
			throw new DeliveryException("Error in cancelDeliveryOrder: " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public void initiateSmartFulfill(DeliveryFulfillRequest deliveryFulfillRequest) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/order/fulfill/smart";
		try {
			deliveryFulfillRequest.setSmartId(smartId);
			log.info("initiateSmartFulfill Request {}", objectMapper.writeValueAsString(deliveryFulfillRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
			Mono<DeliveryFulfillResponse> responses = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(deliveryFulfillRequest)).exchangeToMono(response -> {
						HttpStatus statusCode = (HttpStatus) response.statusCode();
						log.info("Status code: " + statusCode);
						return response.bodyToMono(DeliveryFulfillResponse.class);
					});
			log.info("initiateSmartFulfill Response {}", objectMapper.writeValueAsString(responses));
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
			throw new RuntimeException("Operation failed due to unauthorized access. Token refreshed.", e);
		} catch (Exception e) {
			log.error("Error in initiateSmartFulfill: {}", e);
			throw new DeliveryException("Error in initiateSmartFulfill: " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public DeliveryOrderStatusResponse getDeliveryOrderStatus(String deliveryOrderId) throws DeliveryException {
		try {

			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
			String endpoint = "v1.0/store/channel/vendor/order/" + deliveryOrderId;
			log.info("getDeliveryStatus endPoint {}", endpoint);
			Mono<DeliveryOrderStatusResponse> deliveryOrderStatusResponse = webClient.get().uri(endpoint).retrieve()
					.bodyToMono(DeliveryOrderStatusResponse.class);
			log.info("getDeliveryStatus Response {}", objectMapper.writeValueAsString(deliveryOrderStatusResponse));
			return deliveryOrderStatusResponse.block();
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
			throw new RuntimeException("Operation failed due to unauthorized access. Token refreshed.", e);
		} catch (Exception e) {
			log.error("Error in getDeliveryOrderStatus: {}", e);
			throw new DeliveryException("Error in getDeliveryOrderStatus: " + e.getMessage());
		}
	}

	public void processDeliveryCallback(Delivery delivery, DeliveryOrderStatus deliveryOrderData)
			throws DeliveryException {
		try {
			delivery.setStatus(DeliveryOrderStatusType.getDeliveryOrderStatus(deliveryOrderData.getStatus()));
			Order order = orderService.findById(delivery.getOrderId());

			if (delivery.getStatus() == DeliveryOrderStatusType.CANCELLED) {
				orderService.updateOrderStatus(order.getId(),
						OrderStatusType.getOrderStatusByDelvieryStatus(DeliveryFulfillStatusType.CANCELLED));
				this.unAllocateOrderFulfill(delivery.getDeliveryOrderId());
				this.processDeliveryFulfill(delivery, Constants.ON_CANCEL);
			}
			if (delivery.getStatus() == DeliveryOrderStatusType.FULFILLED
					|| delivery.getStatus() == DeliveryOrderStatusType.COMPLETED) {
				DeliveryFulfillment deliveryFulfill = deliveryOrderData.getFulfillment();
				DeliveryFulfillStatusType fullFillStatus = deliveryFulfill.getStatus();
				delivery.setFulfillment(deliveryFulfill);
				if (deliveryOrderData.getFulfillment().getTrackCode() != null) {
					delivery.getFulfillment().setTrackCode(deliveryOrderData.getFulfillment().getTrackCode());
					order.setDeliveryTrackingLink("https://t.pidge.in?t=" + delivery.getFulfillment().getTrackCode());
				}
				orderService.save(order);
				orderService.updateOrderStatus(order.getId(),
						OrderStatusType.getOrderStatusByDelvieryStatus(fullFillStatus));
				if (posService.isPosUpdateRequired(fullFillStatus)) {
					posService.updatePosRiderStatus(delivery, order);
				}
			}
			this.save(delivery);
		} catch (Exception e) {
			MailNotificationRequest notificationRequest = new MailNotificationRequest("Delivery Error Notification",
					String.format("Error on Process Delivery Callback.\n" + "Order ID: %s\n" + "Error Details: %s",
							delivery.getOrderId(), e.getMessage()));

			mailService.sendNotificationEmail(notificationRequest);
			log.error("Error in processDeliveryCallback: {}", e);
			throw new DeliveryException("Error occurred in processDeliveryCallback: " + e.getMessage());
		}
	}

	public void processDeliveryFulfill(Delivery delivery, String fulfillmentType) throws DeliveryException {
		try {

			DeliveryNetworks selectedNetwork = getServicabilityToken(delivery);
			if (selectedNetwork != null) {
				String token = selectedNetwork.getToken();
				delivery.setNetworkToken(token);
				delivery.setStatus(DeliveryOrderStatusType.FULFILLED);
				delivery.setFulfillmentType(fulfillmentType);
				delivery.setFulfillmentAt(LocalDateTime.now());
				delivery.setNetworkId(selectedNetwork.getNetworkId());
				delivery.setService(selectedNetwork.getService());
				delivery.setPickupNow(selectedNetwork.isPickupNow());
				this.save(delivery);
				this.initiateOrderFulfill(DeliveryRequestTranslation.getOrderFulfillRequest(delivery));
			} else {
				throw new DeliveryException(
						"No matching network found with the specified networkId or minimum price network");
			}

		} catch (DeliveryException e) {
			orderService.updateOrderStatus(delivery.getOrderId(), OrderStatusType.DELIVERY_ERROR);
			throw new DeliveryException(
					"Exception Occured while processDeliveryFulfill in Delivery Service " + e.getMessage());
		}
	}

	public DeliveryNetworks getServicabilityToken(Delivery delivery) throws DeliveryException {
		DeliveryNetworks selectedNetwork = null;
		try {
			DeliveryQuote deliveryQuote = this.getServiceability(delivery.getDeliveryOrderId());
			List<DeliveryNetworks> deliveryNetworks = deliveryQuote.getData().getItems().stream()
					.filter(items -> items.isPickupNow())
					.filter(items -> !items.getService().equalsIgnoreCase("loadshare")).collect(Collectors.toList());

			Optional<DeliveryNetworks> matchingNetworkOpt = deliveryNetworks.stream()
					.filter(network -> network.getNetworkId() == delivery.getNetworkId()).findFirst();

			Optional<DeliveryNetworks> minPriceNetworkOpt = deliveryNetworks.stream()
					.filter(network -> network.getQuote() != null)
					.min(Comparator.comparingDouble(network -> network.getQuote().getPrice()));

			if (matchingNetworkOpt.isPresent() && minPriceNetworkOpt.isPresent()) {
				DeliveryNetworks matchingNetwork = matchingNetworkOpt.get();
				DeliveryNetworks minPriceNetwork = minPriceNetworkOpt.get();
				selectedNetwork = (minPriceNetwork.getQuote().getPrice() < matchingNetwork.getQuote().getPrice())
						? minPriceNetwork
						: matchingNetwork;
			} else if (matchingNetworkOpt.isPresent()) {
				selectedNetwork = matchingNetworkOpt.get();
			} else if (minPriceNetworkOpt.isPresent()) {
				selectedNetwork = minPriceNetworkOpt.get();
			}

		} catch (DeliveryException e) {
			orderService.updateOrderStatus(delivery.getOrderId(), OrderStatusType.DELIVERY_ERROR);
			throw new DeliveryException(
					"Exception Occured while getServicabilityToken in Delivery Service " + e.getMessage());
		}
		return selectedNetwork;
	}

	public void processDeliverySmartFulfill(Delivery delivery, String fulfillmentType) throws DeliveryException {
		try {
			this.initiateSmartFulfill(DeliveryRequestTranslation.getSmartFulfillRequest(delivery));
			delivery.setStatus(DeliveryOrderStatusType.FULFILLED);
			delivery.setFulfillmentType(fulfillmentType);
			delivery.setFulfillmentAt(LocalDateTime.now());
			this.save(delivery);
		} catch (DeliveryException e) {
			orderService.updateOrderStatus(delivery.getOrderId(), OrderStatusType.DELIVERY_ERROR);
			throw new DeliveryException(
					"Exception Occured while processDeliverySmartFulfill in Delivery Service " + e.getMessage());
		}
	}

	@Retryable(retryFor = { Exception.class,WebClientResponseException.Unauthorized.class })
	public void unAllocateOrderFulfill(String deliveryOrderId) throws DeliveryException {
		String endpoint = "/v1.0/store/channel/vendor/" + deliveryOrderId + "/fulfillment/cancel";
		try {
			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.AUTHORIZATION, getToken()).build();
			Mono<Object> responses = webClient.put().uri(endpoint).exchangeToMono(response -> {
				HttpStatus statusCode = (HttpStatus) response.statusCode();
				log.info("Status code: " + statusCode);
				return Mono.just(statusCode);
			});
			log.info("unAllocateOrderFulfill Response {}", objectMapper.writeValueAsString(responses));
		} catch (WebClientResponseException.Unauthorized e) {
			handleUnauthorizedError(e);
		} catch (Exception e) {
			log.error("Error in unAllocateOrderFulfill: {}", e);
			throw new DeliveryException("Error in unAllocateOrderFulfill: " + e.getMessage());
		}
	}

	public String generateToken() {
		String endpoint = "/v1.0/store/channel/vendor/login";
		PidgeLoginRequest requestPayload = new PidgeLoginRequest(pidgeUsername, pidgePassword);

		try {
			log.info("Fetching token for user: {}", pidgeUsername);

			WebClient webClient = WebClient.builder().baseUrl(baseUrl)
					.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();

			Mono<String> createLoginResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(requestPayload)).retrieve().bodyToMono(String.class);

			String response = createLoginResponse.block();

			JsonNode jsonResponse = new ObjectMapper().readTree(response);
			String token = jsonResponse.path("data").path("token").asText();

			log.info("Token fetched successfully: {}", token);
			return token;

		} catch (Exception e) {
			log.error("Error fetching token for user {}: {}", pidgeUsername, e.getMessage(), e);
			throw new RuntimeException("Failed to fetch token", e);
		}
	}

	private void handleUnauthorizedError(WebClientResponseException.Unauthorized e) throws RuntimeException {
		try {
			refreshToken();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to refresh token.", ex);
		}
		throw new RuntimeException("Operation failed due to unauthorized access, Now Token refreshed.", e);
	}

}
