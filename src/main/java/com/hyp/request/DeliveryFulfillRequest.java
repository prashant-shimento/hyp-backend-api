package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class DeliveryFulfillRequest {

    private List<String> ids;
    private String service;

    @JsonProperty("pickup_now")
    private Boolean pickUpNow;

    @JsonProperty("network_id")
    private Integer networkId;

    private String token;

    @JsonProperty("smart_allocation_id")
    private Integer smartId;
}
