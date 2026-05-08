package com.hyp.delivery.adloggs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AdloggsCreateOrderResponse {

    private boolean status;
    private int code;
    private String message;
    private ResponseData data;

    @Data
    @NoArgsConstructor
    public static class ResponseData {

        @JsonProperty("order_uuid")
        private String orderUuid;

        @JsonProperty("trackUrl")
        private String trackUrl;

        @JsonProperty("partner_order_id")
        private String partnerOrderId;

        @JsonProperty("partner_merchant_id")
        private String partnerMerchantId;

        private Double fee;
        private Double distance;

        @JsonProperty("meta_data")
        private String metaData;
    }
}
