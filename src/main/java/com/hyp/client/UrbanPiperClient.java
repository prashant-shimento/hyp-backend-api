package com.hyp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.adapter.urbanpiper.order.UrbanPiperOrderRequest;
import com.hyp.exception.PosException;
import com.hyp.observability.ApplicationMetrics;
import com.hyp.observability.MetricTag;
import com.hyp.observability.MetricsEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrbanPiperClient {

    @Value("${pos.urbanpiper.url:https://api.urbanpiper.com/external/api/v1/}")
    private String baseUrl;

    @Value("${pos.urbanpiper.username:}")
    private String username;

    @Value("${pos.urbanpiper.apikey:}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final ApplicationMetrics metrics;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "apikey " + username + ":" + apiKey)
                .build();
    }

    public String createOrder(UrbanPiperOrderRequest request) throws PosException {
        try {
            log.info("UrbanPiper createOrder request: {}", objectMapper.writeValueAsString(request));
            String response = webClient.post()
                    .uri("orders/")
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("UrbanPiper createOrder response: {}", response);
            metrics.count(MetricsEvent.POS, MetricTag.PARTNER, "URBANPIPER",
                    MetricTag.ACTION, "create", MetricTag.RESULT, "success");
            return response;
        } catch (Exception e) {
            log.error("UrbanPiper createOrder failed: {}", e.getMessage(), e);
            metrics.count(MetricsEvent.POS, MetricTag.PARTNER, "URBANPIPER",
                    MetricTag.ACTION, "create", MetricTag.RESULT, "failed");
            throw new PosException("UrbanPiper order creation failed: " + e.getMessage());
        }
    }

    public String updateRiderStatus(String externalOrderId, Map<String, Object> payload) {
        try {
            log.info("UrbanPiper updateRiderStatus orderId={} payload={}",
                    externalOrderId, objectMapper.writeValueAsString(payload));
            String response = webClient.post()
                    .uri("orders/{id}/rider-status/", externalOrderId)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("UrbanPiper updateRiderStatus response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("UrbanPiper updateRiderStatus failed: {}", e.getMessage(), e);
            return null;
        }
    }
}
