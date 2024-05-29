package com.hyp.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosStockRequest {

	@JsonProperty("restID")
	private String restaurantId;
    private boolean inStock;
    private String type;
	@JsonProperty("itemID")
    private List<String> itemId;
    private String autoTurnOnTime;
    private String customTurnOnTime;
    private String code;
    private String status;
    private String message;

}
