package com.hyp.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class StockRequest {

	private String restID;
    private boolean inStock;
    private String type;
    private List<String> itemID;
    private String autoTurnOnTime;
    private String customTurnOnTime;
    private String code;
    private String status;
    private String message;

}
