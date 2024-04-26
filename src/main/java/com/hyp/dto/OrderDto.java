package com.hyp.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
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
	private double totalAmount;
	@PositiveOrZero(message = "Discount amount must be non-negative")
	private double grandTotalAmount;
	@PositiveOrZero(message = "Grand Total amount must be non-negative")
	private double discountAmount;
	@PositiveOrZero(message = "Tax amount must be positive")
	private double taxAmount;
	@PositiveOrZero(message = "Delivery charge must be positive")
	private double deliveryCharge;
	@PositiveOrZero(message = "Delivery charge tax amount must be positive")
	private double dcTaxAmount;
	@PositiveOrZero(message = "Packaging charge must be positive")
	private double packagingCharge;
	@PositiveOrZero(message = "Packaging charge tax amount must be positive")
	private double pcTaxAmount;
	@PositiveOrZero(message = "Service charge must be positive")
	private double serviceCharge;
	@PositiveOrZero(message = "Service charge tax amount must be positive")
	private double scTaxAmount;
	@Valid
	private List<OrderDiscount> orderDiscount;
	private String discountType;
	private String minDeliveryTime;
	private String minPrepTime;

	@Data
	@NoArgsConstructor
	public static class OrderItem {
		@NotBlank(message = "OrderItem ID cannot be blank")
		private String id;
		@NotBlank(message = "OrderItem name cannot be blank")
		private String name;
		private String description;
		@PositiveOrZero(message = "OrderItem discount must be positive")
		private double itemDiscount;
		@Positive(message = "OrderItem Final price must be positive")
		private double finalPrice;
		@Positive(message = "OrderItem Quantity must be positive")
		private int quantity;
		@NotNull(message = "OrderItem Price must be specified")
		@Positive(message = "OrderItem Price must be positive")
		private double price;
		private String variationName;
		private String variationId;
		private List<OrderItemTax> orderItemTax;
		private List<OrderAddonItem> orderAddonItems;
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
		private double price;

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
		private double price;
		@PositiveOrZero(message = "OrderTax amount must be positive")
		private double tax;
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
		private double price;
	}

	@Data
	@NoArgsConstructor
	public static class OrderItemTax {
		@NotBlank(message = "OrderItemTax ID cannot be blank")
		private String id;
		@NotBlank(message = "OrderItemTax name cannot be blank")
		private String name;
		@PositiveOrZero(message = "OrderItemTax amount must be positive")
		private double amount;
	}

	@Data
	@NoArgsConstructor
	public static class DeliveryDetails {
		@NotBlank(message = "Address ID cannot be blank")
		private String addressId;
		@NotBlank(message = "Service name cannot be blank")
		private String service;
		private String pickUpNow;
		private double networkId;
	}
}
