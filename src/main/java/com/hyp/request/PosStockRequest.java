package com.hyp.request;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosStockRequest {

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
