package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.hyp.exception.NotificationException;
import com.hyp.model.FacebookMessageResponse;
import com.hyp.request.FacebookMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MetaService {

    @Value("${notification.meta.url}")
    private String baseUrl;

    @Value("${notification.meta.app}")
    private String acctId;

    @Value("${notification.meta.token}")
    private String token;

    @Value("${notification.meta.phone}")
    private String phoneId;

    @Value("${notification.meta.acct}")
    private String businessId;

    @Autowired
    ObjectMapper objectMapper;

    public FacebookMessageResponse sendMessage(FacebookMessageRequest facebookMessage) throws NotificationException {
        try {

            log.info("sendMessage Request {}", objectMapper.writeValueAsString(facebookMessage));
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, token)
                    .build();
            String endpoint = "/" + phoneId + "/messages";
            Mono<FacebookMessageResponse> quoteResponseMono = webClient
                    .post()
                    .uri(endpoint)
                    .body(BodyInserters.fromValue(facebookMessage))
                    .retrieve()
                    .bodyToMono(FacebookMessageResponse.class);
            FacebookMessageResponse facebookMessageResponse = quoteResponseMono.block();
            log.info("sendMessage Response {}", objectMapper.writeValueAsString(facebookMessageResponse));
            return facebookMessageResponse;
        } catch (Exception e) {
            log.error("Error in sendMessage {}", e.getMessage());
            throw new NotificationException("Error in sendMessage: " + e.getMessage());
        }
    }
}
