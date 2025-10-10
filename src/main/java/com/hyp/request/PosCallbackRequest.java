package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosCallbackRequest {

    @JsonProperty("restID")
    private String restaurantId;

    @JsonProperty("orderID")
    private String orderId;

    @JsonProperty("cancel_reason")
    private String cancelReason;

    @JsonProperty("minimum_prep_time")
    private String minPrepTime;

    @JsonProperty("minimum_delivery_time")
    private String minDeliveryTime;

    @JsonProperty("rider_name")
    private String riderName;

    @JsonProperty("rider_phone_number")
    private String riderContact;

    @JsonProperty("is_modified")
    private String isModified;

    private String status;
}
