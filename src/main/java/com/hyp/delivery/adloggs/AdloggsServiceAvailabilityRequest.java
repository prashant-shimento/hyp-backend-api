package com.hyp.delivery.adloggs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdloggsServiceAvailabilityRequest {

    @JsonProperty("partner_merchant_id")
    private String partnerMerchantId;

    @JsonProperty("partner_order_id")
    private String partnerOrderId;

    @JsonProperty("pickup_lat")
    private Double pickupLat;

    @JsonProperty("pickup_long")
    private Double pickupLong;

    @JsonProperty("pickup_pincode")
    private String pickupPincode;

    @JsonProperty("delivery_lat")
    private Double deliveryLat;

    @JsonProperty("delivery_long")
    private Double deliveryLong;

    @JsonProperty("delivery_pincode")
    private String deliveryPincode;

    @Builder.Default
    @JsonProperty("utc_offset")
    private int utcOffset = 330;

    @Builder.Default
    @JsonProperty("payment_type")
    private String paymentType = "Online";
}
