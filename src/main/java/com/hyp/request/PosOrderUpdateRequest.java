package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosOrderUpdateRequest extends PosBaseRequest {

    @JsonProperty("restID")
    private String restaurantId;

    @JsonProperty("orderID")
    private String orderId;

    @JsonProperty("clientorderID")
    private String clientOrderId;

    private String cancelReason;

    private String status;
}
