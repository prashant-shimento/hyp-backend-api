package com.hyp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.hyp.constants.Constants;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.model.Location;
import com.hyp.model.RiderLocation;
import com.hyp.request.DeliveryFulfillRequest;
import com.hyp.request.DeliveryFulfillResponse;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.request.PidgeLoginRequest;
import com.hyp.service.MockService;
import com.hyp.service.NotificationService;
import com.hyp.service.OneSignalAlertService;
import com.hyp.service.RedisService;
import com.hyp.util.CommonUtils;
import com.hyp.util.LoggingUtils;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class PidgeClient {

    @Value("${delivery.pidge.url}")
    private String baseUrl;

    @Value("${delivery.pidge.username}")
    private String pidgeUsername;

    @Value("${delivery.pidge.password}")
    private String pidgePassword;

    @Value("${delivery.pidge.smart.id}")
    private Integer smartId;

    private static final int MAX_RETRIES = 3;
    private static final Duration BACKOFF_DURATION = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF_DURATION = Duration.ofSeconds(6);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    RetryTemplate retryTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MockService mockService;

    @Autowired
    private Environment env;

    @Autowired
    private OneSignalAlertService oneSignalAlertService;

    private String getToken() throws DeliveryException {
        return redisService.getRedisData(Constants.REDIS_KEY_PIDGE_TOKEN).orElseThrow(() -> {
            log.error("Token not found in Redis for key: pidgeToken");
            return new DeliveryException("Token not found in Redis");
        });
    }

    private WebClient getClient() throws DeliveryException {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, getToken())
                .build();
    }

    private <T> Mono<T> executeWithRetry(Mono<T> mono) {
        return mono.retryWhen(Retry.backoff(MAX_RETRIES, BACKOFF_DURATION)
                .maxBackoff(MAX_BACKOFF_DURATION)
                .filter(throwable -> throwable instanceof WebClientResponseException.Unauthorized)
                .doBeforeRetry(signal -> {
                    log.warn("Retrying due to Unauthorized error, attempt {}", signal.totalRetries() + 1);
                    refreshToken();
                }));
    }

    public Mono<String> createDeliveryOrder(DeliveryOrderRequest deliveryOrderRequest) throws DeliveryException {
        String endpoint = "/v1.0/store/channel/vendor/order";
        LoggingUtils.logRequest("createDeliveryOrder", deliveryOrderRequest);
        return executeWithRetry(getClient()
                        .post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(deliveryOrderRequest))
                        .retrieve()
                        .bodyToMono(String.class))
                .flatMap(response -> extractOrderId(response, deliveryOrderRequest))
                .doOnSuccess(orderId -> log.info("createDeliveryOrder successful for DeliveryOrderId: {}", orderId))
                .onErrorResume(e -> {
                    log.error("Error in createDeliveryOrder. Cause: {}", e.getMessage(), e);
                    String currentEnv = env.getProperty("env");
                    if ("staging".equalsIgnoreCase(currentEnv) || "dev".equalsIgnoreCase(currentEnv)) {
                        log.warn("Returning mocked DeliveryOrderId due to failure in {} environment", currentEnv);
                        return Mono.just(CommonUtils.generateMockDeliveryOrderId());
                    }
                    DeliveryException deliveryException = new DeliveryException("createDeliveryOrder", e.getMessage());
                    sendAlert(deliveryException);
                    return Mono.error(deliveryException);
                });
    }

    public Mono<Void> fulfillDeliveryOrder(DeliveryFulfillRequest deliveryFulfillRequest) throws DeliveryException {
        String endpoint = "/v1.0/store/channel/vendor/order/fulfill";
        LoggingUtils.logRequest("fulfillDeliveryOrder", deliveryFulfillRequest);

        return executeWithRetry(getClient()
                        .post()
                        .uri(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(deliveryFulfillRequest))
                        .retrieve()
                        .bodyToMono(Void.class))
                .doOnSuccess(unused -> log.info(
                        "fulfillDeliveryOrder completed successfully for DeliveryOrderId: {}",
                        deliveryFulfillRequest.getIds().toString()))
                .onErrorResume(e -> {
                    log.error("Final failure after retries. Error: {}", e.getMessage(), e);
                    String currentEnv = env.getProperty("env");
                    if ("staging".equalsIgnoreCase(currentEnv) || "dev".equalsIgnoreCase(currentEnv)) {
                        log.warn(
                                "Mocking fulfillDeliveryOrder for environment: {} with request: {}",
                                currentEnv,
                                deliveryFulfillRequest);
                        return Mono.empty();
                    }
                    DeliveryException deliveryException = new DeliveryException(
                            "fulfillDeliveryOrder" + " "
                                    + deliveryFulfillRequest.getIds().toString(),
                            e.getMessage());
                    sendAlert(deliveryException);
                    return Mono.error(deliveryException);
                });
    }

    public Mono<Void> cancelDeliveryOrder(String deliveryOrderId) throws DeliveryException {
        String endpoint = "/v1.0/store/channel/vendor/" + deliveryOrderId + "/cancel";
        LoggingUtils.logRequest("cancelDeliveryOrder", endpoint);
        return executeWithRetry(getClient()
                .post()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(unused ->
                        log.info("cancelDeliveryOrder completed successfully for DeliveryOrderId: {}", deliveryOrderId))
                .doOnError(e -> log.error(
                        "Error in cancelDeliveryOrder for DeliveryOrderId {}: {}", deliveryOrderId, e.getMessage(), e))
                .onErrorMap(e -> {
                    log.error(
                            "Final failure after retries for DeliveryOrderId {}. Alerting about failure...",
                            deliveryOrderId);
                    DeliveryException deliveryException =
                            new DeliveryException("cancelDeliveryOrder" + " " + deliveryOrderId, e.getMessage());
                    sendAlert(deliveryException);
                    return deliveryException;
                }));
    }

    public Mono<Object> smartFulfillDeliveryOrder(DeliveryFulfillRequest deliveryFulfillRequest)
            throws DeliveryException {
        String endpoint = "/v1.0/store/channel/vendor/order/fulfill/smart";
        deliveryFulfillRequest.setSmartId(smartId);
        LoggingUtils.logRequest("smartFulfillDeliveryOrder", deliveryFulfillRequest);

        return executeWithRetry(getClient()
                .post()
                .uri(endpoint)
                .body(BodyInserters.fromValue(deliveryFulfillRequest))
                .retrieve()
                .bodyToMono(DeliveryFulfillResponse.class)
                .flatMap(response -> {
                    LoggingUtils.logResponse("smartFulfillDeliveryOrder", response);
                    if (!"Allocation successful"
                            .equalsIgnoreCase(response.getData().getMessage())) {
                        String errorMessage = "Order not fulfilled for IDs: " + deliveryFulfillRequest.getIds();
                        log.error(errorMessage);
                        return Mono.error(new DeliveryException(errorMessage));
                    }
                    return Mono.empty();
                })
                .doOnError(e ->
                        log.error("Error in smartFulfillDeliveryOrder for IDs: {}", deliveryFulfillRequest.getIds(), e))
                .onErrorResume(e -> {
                    log.error("Final failure after retries for smartFulfillDeliveryOrder. Alerting failure...");
                    String currentEnv = env.getProperty("env");
                    if ("staging".equalsIgnoreCase(currentEnv) || "dev".equalsIgnoreCase(currentEnv)) {
                        log.warn(
                                "Mocking smartFulfillDeliveryOrder for environment: {} with request: {}",
                                currentEnv,
                                deliveryFulfillRequest);
                        return Mono.empty();
                    }
                    DeliveryException deliveryException = new DeliveryException(
                            "smartFulfillDeliveryOrder "
                                    + deliveryFulfillRequest.getIds().toString(),
                            e.getMessage());
                    sendAlert(deliveryException);
                    return Mono.error(deliveryException);
                }));
    }

    public Mono<Void> unallocateDeliveryOrder(String deliveryOrderId) throws DeliveryException {
        String endpoint = "/v1.0/store/channel/vendor/" + deliveryOrderId + "/fulfillment/cancel";
        LoggingUtils.logRequest("unallocateDeliveryOrder", endpoint);
        return executeWithRetry(getClient()
                .put()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(unused -> log.info(
                        "unallocateDeliveryOrder completed successfully for DeliveryOrderId: {}", deliveryOrderId))
                .doOnError(
                        e -> log.error("Error in unallocateDeliveryOrder for DeliveryOrderId: {}", deliveryOrderId, e))
                .onErrorMap(e -> {
                    log.error(
                            "Final failure after retries for DeliveryOrderId: {}. Alerting failure...",
                            deliveryOrderId);
                    DeliveryException deliveryException =
                            new DeliveryException("unallocateDeliveryOrder " + deliveryOrderId, e.getMessage());
                    sendAlert(deliveryException);
                    return deliveryException;
                }));
    }

    private Mono<String> extractOrderId(String response, DeliveryOrderRequest deliveryOrderRequest) {
        return Mono.fromCallable(() -> {
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode dataNode = jsonResponse.get("data");
            if (dataNode == null) {
                throw new DeliveryException("Missing 'data' field in response");
            }
            String sourceOrderId = deliveryOrderRequest.getTrips().get(0).getSourceOrderId();
            JsonNode orderIdNode = dataNode.get(sourceOrderId);
            if (orderIdNode == null) {
                throw new DeliveryException("Order ID not found for source order: " + sourceOrderId);
            }

            return orderIdNode.asText();
        });
    }

    @Retryable(retryFor = WebClientResponseException.Unauthorized.class)
    public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) throws DeliveryException {
        String endpoint = "/v1.0/store/channel/vendor/quote";
        try {
            LoggingUtils.logRequest("getDeliveryQuote", deliveryQuoteRequest);
            DeliveryQuote response = getClient()
                    .post()
                    .uri(endpoint)
                    .body(BodyInserters.fromValue(deliveryQuoteRequest))
                    .retrieve()
                    .bodyToMono(DeliveryQuote.class)
                    .doOnNext(res -> LoggingUtils.logResponse("getDeliveryQuote", res))
                    .block();
            return response;
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Unauthorized error. Refreshing token...");
            refreshToken();
            throw new DeliveryException("Unauthorized access. Token refreshed.", e);

        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException occurred while getting quote : {}", e.getMessage(), e);
            DeliveryException deliveryException = new DeliveryException("getDeliveryQuote", e.getMessage());
            String currentEnv = env.getProperty("env");
            if ("staging".equalsIgnoreCase(currentEnv) || "dev".equalsIgnoreCase(currentEnv)) {
                return mockService.readMock("delivery_quote.json", DeliveryQuote.class);
            }
            sendAlert(deliveryException);
            throw deliveryException;

        } catch (Exception e) {
            log.error("Error in getDeliveryQuote: {}", e.getMessage(), e);
            throw new DeliveryException("Error in getDeliveryQuote: " + e.getMessage());
        }
    }

    @Retryable(retryFor = {WebClientResponseException.Unauthorized.class})
    public DeliveryQuote getServiceability(String deliveryOrderId) throws DeliveryException {
        String endpoint = "v1.0/store/channel/vendor/order/fulfillment/services?ids=" + deliveryOrderId;
        try {
            LoggingUtils.logRequest("getServiceability", endpoint);
            DeliveryQuote response = getClient()
                    .get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(DeliveryQuote.class)
                    .doOnNext(res -> LoggingUtils.logResponse("getServiceability", res))
                    .block();
            return response;
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Unauthorized error. Refreshing token...");
            refreshToken();
            throw new DeliveryException("Unauthorized access. Token refreshed.", e);

        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException occurred while getting servicability : {}", e.getMessage(), e);
            DeliveryException deliveryException = new DeliveryException(
                    "getServiceability",
                    "Error in getServiceability api call for orderID " + deliveryOrderId + " : " + e.getMessage());
            sendAlert(deliveryException);
            throw deliveryException;
        } catch (Exception e) {
            log.error("Error in getServiceability: {}", e.getMessage(), e);
            throw new DeliveryException("Error in getServiceability: " + e.getMessage());
        }
    }

    @Retryable(retryFor = {WebClientResponseException.Unauthorized.class})
    public DeliveryRiderLocation getDeliveryRiderLocation(String deliveryOrderId) throws DeliveryException {
        String endpoint = "/v1.0/store/channel/vendor/order/" + deliveryOrderId + "/fulfillment/tracking";
        try {
            LoggingUtils.logRequest("getRiderCurrentLocation", endpoint);
            DeliveryRiderLocation response = getClient()
                    .get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(DeliveryRiderLocation.class)
                    .doOnNext(res -> LoggingUtils.logResponse("getRiderCurrentLocation", res))
                    .block();
            return response;
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Unauthorized error. Refreshing token...");
            refreshToken();
            throw new DeliveryException("Unauthorized access. Token refreshed.", e);

        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException occurred while getting rider location : {}", e.getMessage(), e);
            //			DeliveryException deliveryException = new DeliveryException("getDeliveryRiderLocation",
            //					"Error in getDeliveryRiderLocation api call for orderID " + deliveryOrderId + " : "
            //							+ e.getMessage());
            //			sendAlert(deliveryException);
            //			throw deliveryException;
            throw new DeliveryException("Error in getDeliveryRiderLocation api call for orderID " + deliveryOrderId
                    + " : " + e.getMessage());
        } catch (Exception e) {
            log.error("Error in getRiderCurrentLocation: {}", e.getMessage(), e);
            throw new DeliveryException("Error in getRiderCurrentLocation: " + e.getMessage());
        }
    }

    @Retryable(retryFor = {WebClientResponseException.Unauthorized.class})
    public DeliveryOrderStatus getDeliveryOrderStatus(String deliveryOrderId) throws DeliveryException {
        String endpoint = "v1.0/store/channel/vendor/order/" + deliveryOrderId;
        LoggingUtils.logRequest("getDeliveryOrderStatus", endpoint);
        try {
            DeliveryOrderStatus response = getClient()
                    .get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(DeliveryOrderStatus.class)
                    .doOnNext(res -> LoggingUtils.logResponse("getDeliveryOrderStatus", res))
                    .block();
            return response;

        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("Unauthorized error. Refreshing token...");
            refreshToken();
            throw new DeliveryException("Unauthorized access. Token refreshed.", e);

        } catch (WebClientResponseException e) {
            log.error(
                    "WebClientResponseException occurred while fetching delivery order status: {}", e.getMessage(), e);
            DeliveryException deliveryException = new DeliveryException(
                    "getDeliveryOrderStatus",
                    "Error in getDeliveryOrderStatus api call for orderID " + deliveryOrderId + " : " + e.getMessage());
            sendAlert(deliveryException);
            throw deliveryException;
        } catch (Exception e) {
            log.error("General exception occurred while fetching delivery order status: {}", e.getMessage(), e);
            throw new DeliveryException("Error in getDeliveryOrderStatus: " + e.getMessage(), e);
        }
    }

    public void refreshToken() {
        try {
            String newToken = generateToken();
            if (newToken == null || newToken.isEmpty()) {
                throw new DeliveryException("Failed to refresh token: Received empty token");
            }
            redisService.setRedisData(Constants.REDIS_KEY_PIDGE_TOKEN, newToken, 0);
            log.info("Token refreshed successfully");
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    private String generateToken() throws DeliveryException {
        String endpoint = "/v1.0/store/channel/vendor/login";
        PidgeLoginRequest requestPayload = new PidgeLoginRequest(pidgeUsername, pidgePassword);
        try {
            String response = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri(endpoint)
                    .body(BodyInserters.fromValue(requestPayload))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.path("data").path("token").asText();
        } catch (WebClientResponseException e) {
            log.error(
                    "WebClientResponseException occurred while fetching delivery order status: {}", e.getMessage(), e);
            DeliveryException deliveryException =
                    new DeliveryException("Error in generateToken api call : " + e.getMessage());
            sendAlert(deliveryException);
            throw deliveryException;
        } catch (Exception e) {
            log.error("General exception occurred while generating token : {}", e.getMessage(), e);
            throw new DeliveryException("Error in generateToken: " + e.getMessage(), e);
        }
    }

    public void sendAlert(DeliveryException e) {
        notificationService.sendInternalGroupNotification(
                Constants.META_GENERIC_ALERT_TEMPLATE, List.of(e.getAction(), e.getMessage(), "DELIVERY"));
        oneSignalAlertService.notifyErrorResponseAlert(e.getAction(), e.getMessage(), "DELIVERY");
    }

    @Retryable(retryFor = {WebClientResponseException.Unauthorized.class})
    public RiderLocation getRiderLocation(String deliveryOrderId) throws DeliveryException {
        String endpoint = "v1.0/store/tracking/rider-location?id=" + deliveryOrderId;
        try {
            LoggingUtils.logRequest("getRiderLocation", endpoint);
            RiderLocation response = getClient()
                    .get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(RiderLocation.class)
                    .doOnNext(res -> LoggingUtils.logResponse("getRiderLocation", res))
                    .block();
            return response;
        } catch (Exception e) {
            log.error("Error in getRiderLocation: {}", e.getMessage(), e);
            throw new DeliveryException("Error in getRiderLocation: " + e.getMessage());
        }
    }

    @Retryable(retryFor = {WebClientResponseException.Unauthorized.class})
    public RiderLocation getPorterRiderLocation(String orderId) throws DeliveryException {
        String customerUuid = "c64094ca-5f13-4aeb-9a81-f9f0af5e43c1";
        String endpoint = String.format(
                "https://porter.in/api/public/orders/order_details?order_id=%s&customer_uuid=%s",
                orderId, customerUuid);

        try {
            LoggingUtils.logRequest("getPorterRiderLocation", endpoint);

            JsonNode root = getClient()
                    .get()
                    .uri(endpoint)
                    .headers(headers -> {
                        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                        headers.set("Accept", "application/json, text/plain, */*");
                    })
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnNext(res -> LoggingUtils.logResponse("getPorterRiderLocation", res))
                    .block();

            if (root == null || root.isMissingNode()) {
                log.warn("Null or missing response for orderId: {}", orderId);
                return null;
            }

            JsonNode partnerLocation = root.path("order_details").path("partner_location");
            if (partnerLocation.isMissingNode()) {
                log.warn("No partner location found in response for orderId: {}", orderId);
                return null;
            }

            double lat = partnerLocation.path("lat").asDouble();
            double lng = partnerLocation.path("long").asDouble();
            return RiderLocation.builder()
                    .data(Location.builder().latitude(lat).longitude(lng).build())
                    .build();

        } catch (WebClientResponseException.Unauthorized ex) {
            log.error("Unauthorized access while fetching rider location for orderId: {}", orderId, ex);
            throw new DeliveryException("Unauthorized access to Porter API");
        } catch (Exception ex) {
            log.error("Error in getPorterRiderLocation for orderId: {}", orderId, ex);
            throw new DeliveryException("Failed to fetch rider location: " + ex.getMessage());
        }
    }
}
