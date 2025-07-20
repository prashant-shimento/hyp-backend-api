package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class PosStatusRequest {

	@JsonProperty("restaurant_id")
	private String restaurantId;
	@JsonProperty("restID")
	private String menuSharingCode;
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
