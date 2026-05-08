package com.hyp.delivery.adloggs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdloggsWebhookPayload {

    @JsonProperty("order_uuid")
    private String orderUuid;

    @JsonProperty("order_status_id")
    private Integer orderStatusId;

    @JsonProperty("partner_order_id")
    private String partnerOrderId;

    @JsonProperty("deliveryStaffDetails")
    private DeliveryStaffDetails deliveryStaffDetails;

    private Eta eta;

    @JsonProperty("rider_platform")
    private RiderPlatform riderPlatform;

    private String reason;
    private Otps otps;

    @Data
    @NoArgsConstructor
    public static class DeliveryStaffDetails {
        private String name;
        private String phone;

        @JsonProperty("currentLocation")
        private CurrentLocation currentLocation;
    }

    @Data
    @NoArgsConstructor
    public static class CurrentLocation {
        private String lat;

        @JsonProperty("long")
        private String lng;
    }

    @Data
    @NoArgsConstructor
    public static class Eta {

        @JsonProperty("to_pickup")
        private Integer toPickup;

        @JsonProperty("to_delivery")
        private Integer toDelivery;
    }

    @Data
    @NoArgsConstructor
    public static class RiderPlatform {
        private String name;

        @JsonProperty("lsp_uniq_id")
        private String lspUniqId;
    }

    @Data
    @NoArgsConstructor
    public static class Otps {

        @JsonProperty("delivery_otp")
        private String deliveryOtp;

        @JsonProperty("pickup_otp")
        private String pickupOtp;

        @JsonProperty("return_otp")
        private String returnOtp;
    }
}
