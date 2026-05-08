package com.hyp.delivery.adloggs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdloggsCreateOrderRequest {

    @JsonProperty("partner_order_id")
    private String partnerOrderId;

    @JsonProperty("partner_merchant_id")
    private String partnerMerchantId;

    @JsonProperty("pickup_contact_name")
    private String pickupContactName;

    @JsonProperty("pickup_contact_no")
    private String pickupContactNo;

    @JsonProperty("pickup_contact_email")
    private String pickupContactEmail;

    @JsonProperty("pickup_address")
    private String pickupAddress;

    @JsonProperty("pickup_address_details")
    private AddressDetails pickupAddressDetails;

    @JsonProperty("pickup_date_time")
    private String pickupDateTime;

    @JsonProperty("pickup_lat")
    private Double pickupLat;

    @JsonProperty("pickup_long")
    private Double pickupLong;

    @JsonProperty("delivery_contact_name")
    private String deliveryContactName;

    @JsonProperty("delivery_contact_no")
    private String deliveryContactNo;

    @JsonProperty("delivery_contact_email")
    private String deliveryContactEmail;

    @JsonProperty("delivery_address")
    private String deliveryAddress;

    @JsonProperty("delivery_address_details")
    private AddressDetails deliveryAddressDetails;

    @JsonProperty("delivery_lat")
    private Double deliveryLat;

    @JsonProperty("delivery_long")
    private Double deliveryLong;

    @Builder.Default
    @JsonProperty("order_category")
    private String orderCategory = "Food and Beverage";

    @JsonProperty("order_total_price")
    private String orderTotalPrice;

    @Builder.Default
    @JsonProperty("order_total_weight_in_kg")
    private String orderTotalWeightInKg = "1.0";

    @JsonProperty("items")
    private List<OrderItem> items;

    @JsonProperty("order_description")
    private String orderDescription;

    @Builder.Default
    @JsonProperty("utc_offset")
    private int utcOffset = 330;

    @Builder.Default
    @JsonProperty("payment_type")
    private String paymentType = "Online";

    @Builder.Default
    @JsonProperty("collectible_amount")
    private double collectibleAmount = 0;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddressDetails {

        @JsonProperty("door_no")
        private String doorNo;

        @JsonProperty("street_name")
        private String streetName;

        @JsonProperty("city_name")
        private String cityName;

        @JsonProperty("district_name")
        private String districtName;

        @JsonProperty("state_name")
        private String stateName;

        @JsonProperty("country_name")
        private String countryName;

        private String pincode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String name;
        private String quantity;
        private String price;
    }
}
