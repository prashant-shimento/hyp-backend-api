package com.hyp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;

import reactor.core.publisher.Mono;

@Service
public class PosOrderServiceImpl implements PosOrderService {

	@Value("${pos.petpooja.url}")
	private String baseUrl;

	@Override
	public String createOrder(PosOrderRequest posOrderRequest) {
		try {
			System.out.println("Request "+new ObjectMapper().writeValueAsString(posOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/save_order";
			Mono<String> saveOrderResponse = webClient.post().uri(endpoint).body(BodyInserters.fromValue(posOrderRequest))
					.retrieve().bodyToMono(String.class);
			saveOrderResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return saveOrderResponse.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}

	@Override
	public String updateOrder(PosOrderUpdateRequest posOrderUpdateRequest) {
		try {
			System.out.println("Request "+new ObjectMapper().writeValueAsString(posOrderUpdateRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/update_order_status";
			Mono<String> updateOrderResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(posOrderUpdateRequest)).retrieve().bodyToMono(String.class);
			updateOrderResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return updateOrderResponse.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	@Override
	public String updateRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest) {
		try {
			System.out.println("Request "+new ObjectMapper().writeValueAsString(posRiderUpdateRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/rider_status_update";
			Mono<String> riderUpdateResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(posRiderUpdateRequest)).retrieve().bodyToMono(String.class);
			riderUpdateResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return riderUpdateResponse.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}
