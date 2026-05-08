package com.hyp.delivery.adloggs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdloggsServiceAvailabilityResponse {

    private boolean status;
    private int code;
    private String message;
    private ResponseData data;

    @Data
    @NoArgsConstructor
    public static class ResponseData {

        @JsonProperty("service_available")
        private boolean serviceAvailable;

        @JsonProperty("to_pickup")
        private ToPickup toPickup;

        private Double distance;

        @JsonProperty("estimated_price")
        private Double estimatedPrice;

        private String code;
    }

    @Data
    @NoArgsConstructor
    public static class ToPickup {

        @JsonProperty("eta_min")
        private Integer etaMin;
    }
}
