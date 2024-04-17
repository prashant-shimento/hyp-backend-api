package com.hyp.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFulfillRequest {
	
	private List<String> ids;
	private String service;
	@JsonProperty("pickup_now")
	private boolean pickUpNow;
	@JsonProperty("network_id")
	private int networkId;
	private String token;
}
