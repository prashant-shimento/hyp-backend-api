package com.hyp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyp.dto.*;
import com.hyp.entity.Delivery;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerInvoice {

    @JsonProperty("invoice_number")
    public String invoiceNumber;

    public String status;

    @JsonProperty("order_time")
    public String orderTime;

    public CustomerDto customer;
    public Restaurant restaurant;
    public List<OrderItem> items;

    @JsonProperty("discount_amount")
    public Double discountAmount;

    @JsonProperty("cgst_amount")
    public Double cgstAmount;

    @JsonProperty("sgst_amount")
    public Double sgstAmount;

    @JsonProperty("total_amount")
    public Double totalAmount;

    @JsonProperty("delivery_charge")
    public Double deliveryCharge;

    @JsonProperty("grand_total_amount")
    public Double grantTotalAmount;

    public Delivery delivery;

    @Data
    @NoArgsConstructor
    public static class OrderItem {
        private String name;
        private int quantity;
        private Double price;

        @JsonProperty("item_tax")
        private List<ItemTax> itemTax;
    }

    @Data
    @NoArgsConstructor
    public static class Restaurant {
        private String name;
        private String address;
        private String contact;
    }

    @Data
    @NoArgsConstructor
    public static class ItemTax {
        private String id;
        private String name;
        private Double amount;
    }

    @Data
    @NoArgsConstructor
    public static class Delivery {
        private Address address;
    }

    @Data
    @NoArgsConstructor
    public static class Address {
        @JsonProperty("address_line_1")
        private String addressLine1;

        @JsonProperty("address_line_2")
        private String addressLine2;

        private String city;
        private String state;
        private String country;
        private String pincode;
        private double latitude;
        private double longitude;
    }
}
