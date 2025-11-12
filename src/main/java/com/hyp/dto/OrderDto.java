package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.enums.OrderPlatform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class OrderDto extends BaseDto {

    @NotBlank(message = "Customer ID cannot be blank")
    private String customerId;

    @NotBlank(message = "Order type cannot be blank")
    private String orderType;

    @NotBlank(message = "Payment type cannot be blank")
    private String paymentType;

    private String description;

    @Valid
    @Size(min = 1, message = "At least one order item must be present")
    private List<OrderItem> orderItems;

    @Valid
    private List<OrderTax> orderTax;

    @Valid
    private DeliveryDetails deliveryDetails;

    @NotBlank(message = "Special instructions cannot be blank")
    private String specialInstructions;

    @NotBlank(message = "Order time cannot be blank")
    private String orderTime;

    private String expectedDeliveryTime;
    private String status;

    @Positive(message = "Total amount must be positive")
    private Double totalAmount;

    @PositiveOrZero(message = "Discount amount must be non-negative")
    private Double grandTotalAmount;

    @PositiveOrZero(message = "Grand Total amount must be non-negative")
    private Double discountAmount;

    @PositiveOrZero(message = "Tax amount must be positive")
    private Double taxAmount;

    @PositiveOrZero(message = "Delivery charge must be positive")
    private Double deliveryCharge;

    @PositiveOrZero(message = "Delivery charge tax amount must be positive")
    private Double dcTaxAmount;

    @PositiveOrZero(message = "Packaging charge must be positive")
    private Double packagingCharge;

    @PositiveOrZero(message = "Packaging charge tax amount must be positive")
    private Double pcTaxAmount;

    @PositiveOrZero(message = "Service charge must be positive")
    private Double serviceCharge;

    @PositiveOrZero(message = "Service charge tax amount must be positive")
    private Double scTaxAmount;

    private LocalDateTime preOrderDateTime;

    private boolean preOrder;

    @Valid
    private List<OrderDiscount> orderDiscount;

    private String discountType;
    private String minDeliveryTime;
    private String minPrepTime;
    private String deliveryTrackingLink;
    private String screen;
    private String seat;
    private String paymentOrderId;
    private List<OrderLog> orderLogs = new ArrayList<>();
    private OrderCase orderCase;
    private Double platformFee;
    private OrderPlatform orderPlatform;
    private double itemTotalAmount;

    @Data
    @NoArgsConstructor
    @JsonInclude(Include.NON_NULL)
    public static class OrderItem {
        @NotBlank(message = "OrderItem ID cannot be blank")
        private String id;

        @NotBlank(message = "OrderItem name cannot be blank")
        private String name;

        private String description;

        @PositiveOrZero(message = "OrderItem discount must be positive")
        private Double itemDiscount;

        @Positive(message = "OrderItem Final price must be positive")
        private Double finalPrice;

        @Positive(message = "OrderItem Quantity must be positive")
        private int quantity;

        @NotNull(message = "OrderItem Price must be specified")
        @Positive(message = "OrderItem Price must be positive")
        private Double price;

        private String variationName;
        private String variationId;
        private List<OrderItemTax> orderItemTax;
        private List<OrderAddonItem> orderAddonItems;
        private String itemAttribute;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(Include.NON_NULL)
    public static class OrderAddonItem {
        @NotBlank(message = "OrderAddonItem item ID cannot be blank")
        private String addonItemId;

        @NotBlank(message = "OrderAddonItem item name cannot be blank")
        private String addonItemName;

        private String addonGroupName;
        private String addonGroupId;

        @PositiveOrZero(message = " OrderAddonItem Quantity must be positive")
        private int quantity;

        @Positive(message = "OrderAddonItem price must be positive")
        private Double price;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(Include.NON_NULL)
    public static class OrderTax {

        @NotNull(message = "OrderTax id title cannot be blank")
        private String id;

        @NotNull(message = "OrderTax title cannot be blank")
        private String title;

        private String type;

        @Positive(message = "OrderTax price must be positive")
        private Double price;

        @PositiveOrZero(message = "OrderTax amount must be positive")
        private Double tax;

        private Double restaurantLiableAmt;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(Include.NON_NULL)
    public static class OrderDiscount {
        @NotBlank(message = "OrderDiscount id cannot be blank")
        private String id;

        @NotBlank(message = "OrderDiscount title cannot be blank")
        private String title;

        private String type;

        @Positive(message = "OrderDiscount price must be positive")
        private Double price;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(Include.NON_NULL)
    public static class OrderItemTax {
        @NotBlank(message = "OrderItemTax ID cannot be blank")
        private String id;

        @NotBlank(message = "OrderItemTax name cannot be blank")
        private String name;

        @PositiveOrZero(message = "OrderItemTax amount must be positive")
        private Double amount;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(Include.NON_NULL)
    public static class DeliveryDetails {
        private String addressId;
        private String service;
        private String pickUpNow;
        private Double networkId;
    }

    @Data
    @NoArgsConstructor
    public static class OrderLog {
        private String status;
        private LocalDateTime loggedAt;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(Include.NON_NULL)
    public static class OrderCase {
        private boolean isCase;
        private String details;
        private String createdBy;
        private String updatedBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String resolution;
        private String status;
    }
}
