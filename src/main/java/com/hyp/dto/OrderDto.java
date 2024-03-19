package com.hyp.dto;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;

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
	private String orderType;
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
	private double dcTaxAmount;
	private double packagingCharge;
	private double pcTaxAmount;
	private double serviceCharge;
	private double scTaxAmount;
	private OrderDiscount orderDiscount;

	@Data
	@NoArgsConstructor
	public static class OrderItem {
		private String id;
		private String name;
		private String description;
		private double itemDiscount;
		private double finalPrice;
		private int quantity;
		private double price;
		private String variationName;
		private String variationId;
		private List<OrderItemTax> orderItemTax;	
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
		private String id;
		private String title;
	    private String type;
	    private double price;
	    private double tax;
	}
	
	@Data
	@NoArgsConstructor
	public static class OrderDiscount {
		private String id;
		private String title;
	    private String type;
	    private double price;
	}
	
	@Data
	@NoArgsConstructor
	public static class OrderItemTax {
		private String id;
		private String name;
	    private double amount;
	}
}
