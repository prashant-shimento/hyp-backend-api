package com.hyp.request.urbanpiper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreStatusRequest {
    
    @JsonProperty("location_ref_id")
    private String locationRefId;
    
    @JsonProperty("ordering_enabled")
    private Boolean orderingEnabled;
}
