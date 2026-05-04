package com.hyp.adapter.urbanpiper.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UrbanPiperOrderStatusRequest {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("external_order_id")
    private String externalOrderId;

    private String status;

    @JsonProperty("status_message")
    private String statusMessage;

    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("location_ref_id")
    private String locationRefId;
}
