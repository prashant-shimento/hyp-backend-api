package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class DeliveryFulfillResponse {

    private Data data;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data {
        private boolean fulfilled;
        private String message;

        @JsonProperty("network_id")
        private int networkId;

        @JsonProperty("network_name")
        private String networkName;

        private Quote quote;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Quote {
        private double price;
        private double distance;
        private ETA eta;

        @JsonProperty("price_breakup")
        private PriceBreakup priceBreakup;

        @JsonProperty("is_rain")
        private boolean isRain;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ETA {
        private String pickup;

        @JsonProperty("pickup_min")
        private double pickupMin;

        private String drop;

        @JsonProperty("drop_min")
        private double dropMin;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PriceBreakup {
        @JsonProperty("base_delivery_charge")
        private double baseDeliveryCharge;

        @JsonProperty("total_gst_amount")
        private double totalGstAmount;

        private double surge;

        @JsonProperty("additional_charges")
        private List<Object> additionalCharges;

        private List<Item> items;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Item {
        @JsonProperty("order_id")
        private String orderId;

        private double total;
        private double amount;
        private double tax;
    }
}
