package com.hyp.adapter.urbanpiper.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class InventoryUpdateRequest {

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
