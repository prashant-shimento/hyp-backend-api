package com.hyp.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.entity.Attribute;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=false)
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
	@Valid
	private List<OrderDiscount> orderDiscount;
	private String discountType;
	private String minDeliveryTime;
	private String minPrepTime;
	private String deliveryTrackingLink;
	private String screen;
	private String seat;
	
	@Data
	@NoArgsConstructor
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
	public static class DeliveryDetails {
		@NotBlank(message = "Address ID cannot be blank")
		private String addressId;
		private String service;
		private String pickUpNow;
		private Double networkId;
	}
}
