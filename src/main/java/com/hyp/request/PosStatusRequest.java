package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosStatusRequest {

	@JsonProperty("restID")
	private String restaurantId;
    private String status;
	@JsonProperty("store_status")
	private String storeStatus;
	@JsonProperty("turn_on_time")
    private String turnOnTime;
    private String reason;
	@JsonProperty("http_code")
    private String httpCode;
    private String message;

}
