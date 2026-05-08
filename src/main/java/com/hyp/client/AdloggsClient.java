package com.hyp.client;

import com.hyp.constants.Constants;
import com.hyp.delivery.adloggs.AdloggsCreateOrderRequest;
import com.hyp.delivery.adloggs.AdloggsCreateOrderResponse;
import com.hyp.delivery.adloggs.AdloggsServiceAvailabilityRequest;
import com.hyp.delivery.adloggs.AdloggsServiceAvailabilityResponse;
import com.hyp.exception.DeliveryException;
import com.hyp.service.NotificationService;
import com.hyp.service.OneSignalAlertService;
import com.hyp.util.LoggingUtils;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Component
@Slf4j
public class AdloggsClient {

    private static final String API_KEY_HEADER = "x-api-key";
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);

    private static final String EP_SERVICE_CHECK = "/aa/oporder/v1.2/service/availability";
    private static final String EP_CREATE_ORDER = "/aa/oporder/v2/create";
    private static final String EP_CANCEL_ORDER = "/aa/oporder/v1.2/cancel";

    @Value("${delivery.adloggs.url}")
    private String baseUrl;

    @Value("${delivery.adloggs.api-key}")
    private String apiKey;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private OneSignalAlertService oneSignalAlertService;

    private WebClient webClient;

    @PostConstruct
    private void initWebClient() {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public AdloggsServiceAvailabilityResponse checkServiceAvailability(AdloggsServiceAvailabilityRequest request)
            throws DeliveryException {
        try {
            LoggingUtils.logRequest("adloggs.checkServiceAvailability", request);
            AdloggsServiceAvailabilityResponse response = webClient
                    .post()
                    .uri(EP_SERVICE_CHECK)
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(AdloggsServiceAvailabilityResponse.class)
                    .doOnNext(res -> LoggingUtils.logResponse("adloggs.checkServiceAvailability", res))
                    .block(BLOCK_TIMEOUT);
            return response;
        } catch (WebClientRequestException e) {
            log.error("Connection error in Adloggs checkServiceAvailability: {}", e.getMessage(), e);
            throw new DeliveryException("adloggs.checkServiceAvailability", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error in Adloggs checkServiceAvailability: {}", e.getMessage(), e);
            throw new DeliveryException("adloggs.checkServiceAvailability", e.getMessage(), e);
        }
    }

    public AdloggsCreateOrderResponse createOrder(AdloggsCreateOrderRequest request) throws DeliveryException {
        try {
            LoggingUtils.logRequest("adloggs.createOrder", request);
            AdloggsCreateOrderResponse response = webClient
                    .post()
                    .uri(EP_CREATE_ORDER)
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(AdloggsCreateOrderResponse.class)
                    .doOnNext(res -> LoggingUtils.logResponse("adloggs.createOrder", res))
                    .block(BLOCK_TIMEOUT);

            if (response == null || !response.isStatus()) {
                String msg = response != null ? response.getMessage() : "null response from Adloggs";
                log.error("Adloggs createOrder failed: {}", msg);
                DeliveryException ex = new DeliveryException("adloggs.createOrder", msg);
                sendAlert(ex);
                throw ex;
            }
            log.info(
                    "Adloggs createOrder successful orderUuid={}",
                    response.getData().getOrderUuid());
            return response;
        } catch (DeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in Adloggs createOrder: {}", e.getMessage(), e);
            DeliveryException ex = new DeliveryException("adloggs.createOrder", e.getMessage(), e);
            sendAlert(ex);
            throw ex;
        }
    }

    public void cancelOrder(String orderUuid, String reason) throws DeliveryException {
        Map<String, String> body = Map.of("order_uuid", orderUuid, "order_cancel_description", reason);
        try {
            LoggingUtils.logRequest("adloggs.cancelOrder", body);
            webClient
                    .post()
                    .uri(EP_CANCEL_ORDER)
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(BLOCK_TIMEOUT);
            log.info("Adloggs cancelOrder successful uuid={}", orderUuid);
        } catch (Exception e) {
            log.error("Error in Adloggs cancelOrder uuid={}: {}", orderUuid, e.getMessage(), e);
            throw new DeliveryException("adloggs.cancelOrder", e.getMessage(), e);
        }
    }

    private void sendAlert(DeliveryException e) {
        notificationService.sendInternalGroupNotification(
                Constants.META_GENERIC_ALERT_TEMPLATE, List.of(e.getAction(), e.getMessage(), "DELIVERY_ADLOGGS"));
        oneSignalAlertService.notifyErrorResponseAlert(e.getAction(), e.getMessage(), "DELIVERY_ADLOGGS");
    }
}
