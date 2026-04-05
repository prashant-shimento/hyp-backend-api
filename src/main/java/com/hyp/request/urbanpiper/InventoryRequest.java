package com.hyp.request.urbanpiper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryRequest {

    @JsonProperty("location_ref_id")
    private String locationRefId;

    private List<String> items;

    private List<String> variants;

    @JsonProperty("add_ons")
    private List<String> addOns;

    @JsonProperty("in_stock")
    private Boolean inStock;

    @JsonProperty("next_available_at")
    private Long nextAvailableAt;
}