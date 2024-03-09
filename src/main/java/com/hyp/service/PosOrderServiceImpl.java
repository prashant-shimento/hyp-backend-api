package com.hyp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.hyp.request.PosOrderRequest;

import reactor.core.publisher.Mono;

@Service
public class PosOrderServiceImpl implements PosOrderService {
	
	@Value("${pos.petpooja.url}")
    private String baseUrl;

	@Override
	public void createOrder(PosOrderRequest posOrderRequest) {
		WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
		String endpoint = "/save_order";
		Mono<String> saveOrderResponse = webClient.post().uri(endpoint).body(BodyInserters.fromValue(posOrderRequest)).retrieve().bodyToMono(String.class);
		saveOrderResponse.subscribe(reponse -> {
            System.out.println("Response: " + reponse);
		},error -> {
            System.err.println("Error response: " + error.getMessage());
		});
	}

}
