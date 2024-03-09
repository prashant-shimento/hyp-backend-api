package com.hyp.dto;

import java.util.List;

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

	private String customerId;
	private int orderType;
	private String paymentType;
	private String description;
	private List<OrderItem> orderItems;
	private List<OrderTax> orderTax;
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
	private double serviceCharge;
	private OrderDiscount orderDiscount;

	@Data
	@NoArgsConstructor
	public static class OrderItem {
		private String itemId;
		private String itemName;
		private double itemDiscount;
		private int quantity;
		private double price;
		private String variationName;
		private String variationId;
		private List<OrderAddonItem> orderAddonItems;
	}

	@Data
	@NoArgsConstructor
	public static class OrderAddonItem {
		private String addonItemId;
		private String addonItemName;
		private String addonGroupName;
		private String addonGroupId;
		private int quantity;
		private double price;
		
	}
	
	@Data
	@NoArgsConstructor
	public static class OrderTax {
		private String taxId;
		private String title;
	    private String type;
	    private double price;
	    private double tax;
	}
	
	@Data
	@NoArgsConstructor
	public static class OrderDiscount {
		private String discountId;
		private String title;
	    private String type;
	    private double price;
	}
}
