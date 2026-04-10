package com.hyp.adapter.urbanpiper.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UrbanPiperOrderRequest {

    private Customer customer;
    private List<Item> items;
    private Meta meta;
    private List<Discount> discounts;
    private Payment payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customer {
        private Address address;
        private String email;
        private String name;
        @JsonProperty("phone_number")
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Address {
        private String city;
        private String country;
        private String instructions;
        private String landmark;
        private Double latitude;
        private Double longitude;
        @JsonProperty("line_1")
        private String line1;
        @JsonProperty("line_2")
        private String line2;
        private String pincode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private List<Addon> addons;
        private List<Charge> charges;
        private List<Discount> discounts;
        private Double discount;
        private String instructions;
        @JsonProperty("price_per_unit")
        private Double pricePerUnit;
        private Integer quantity;
        @JsonProperty("ref_id")
        private String refId;
        private Double subtotal;
        private List<Tax> taxes;
        private String title;
        private Double total;
        private List<Variant> variants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Addon {
        @JsonProperty("ref_id")
        private String refId;
        private String title;
        @JsonProperty("price_per_unit")
        private Double pricePerUnit;
        private Integer quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Variant {
        private List<Addon> addons;
        @JsonProperty("price_per_unit")
        private Double pricePerUnit;
        @JsonProperty("ref_id")
        private String refId;
        private String title;
        private Integer quantity;
        private List<Variant> variants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Charge {
        private String title;
        private Double value;
        private List<Tax> taxes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tax {
        @JsonProperty("liability_on")
        private String liabilityOn;
        private String title;
        private Double value;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Discount {
        private String title;
        private String code;
        private Double value;
        private String type;
        @JsonProperty("mechant_sponsored")
        private Boolean merchantSponsored;
        private Double rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private List<Charge> charges;
        private Long created;
        @JsonProperty("current_status")
        private String currentStatus;
        @JsonProperty("discount_code")
        private String discountCode;
        @JsonProperty("fulfillment_mode")
        private String fulfillmentMode;
        private String instructions;
        @JsonProperty("is_edit")
        private Boolean isEdit;
        @JsonProperty("item_level_charges")
        private Double itemLevelCharges;
        @JsonProperty("item_level_discount")
        private Double itemLevelDiscount;
        @JsonProperty("item_level_taxes")
        private Double itemLevelTaxes;
        @JsonProperty("location_ref_id")
        private String locationRefId;
        @JsonProperty("order_level_charges")
        private Double orderLevelCharges;
        @JsonProperty("order_level_discount")
        private Double orderLevelDiscount;
        @JsonProperty("is_instant_order")
        private Boolean isInstantOrder;
        @JsonProperty("prep_time_details")
        private PrepTimeDetails prepTimeDetails;
        @JsonProperty("order_no")
        private String orderNo;
        @JsonProperty("restaurant_name")
        private String restaurantName;
        @JsonProperty("sub_total")
        private Double subTotal;
        private Double total;
        @JsonProperty("total_charges")
        private Double totalCharges;
        @JsonProperty("total_discount")
        private Double totalDiscount;
        @JsonProperty("total_taxes")
        private Double totalTaxes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrepTimeDetails {
        @JsonProperty("predicted_prep_time")
        private Integer predictedPrepTime;
        @JsonProperty("max_increase_threshold")
        private Integer maxIncreaseThreshold;
        @JsonProperty("max_decrease_threshold")
        private Integer maxDecreaseThreshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        @JsonProperty("amount_balance")
        private Double amountBalance;
        @JsonProperty("amount_paid")
        private Double amountPaid;
        private String mode;
        private String status;
    }
}
