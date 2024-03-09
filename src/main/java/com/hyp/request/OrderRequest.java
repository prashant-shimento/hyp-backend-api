package com.hyp.request;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderRequest {
	private String customerId;
	private String restaurantId;
	private String orderType;
	private String paymentMethod;
	private List<OrderItem> items;
	private String deliveryAddress;
	private String specialInstructions;
	private String orderTime;
	private String expectedDeliveryTime;
	private String status;
	private double totalAmount;
	private double discountAmount;
	private double taxAmount;
	private double deliveryCharge;
	private double packagingCharge;

	@Data
	@NoArgsConstructor
	public class OrderItem {
		private String itemId;
		private String itemName;
		private int quantity;
		private double price;
		private List<Variation> variations;
		private List<Addon> addons;
	}

	@Data
	@NoArgsConstructor
	public class Variation {
		private String variationId;
		private String variationName;
		private double price;
	}

	@Data
	@NoArgsConstructor
	public class Addon {
		private String addonId;
		private String addonName;
		private double price;
	}

}
