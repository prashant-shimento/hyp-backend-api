package com.hyp.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hyp.enums.CaseStatusType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.annotation.GenerateId;
import com.hyp.enums.OrderStatusType;
import com.hyp.enums.PaymentType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "orders")
public class Order {

	@Id
	@Field("id")
	@GenerateId(sequenceName = "order_sequence")
	private String id;

	@Field("customer_id")
	private String customerId;

	@Field("order_type")
	private String orderType;

	@Field("payment_type")
	private PaymentType paymentType;

	@Field("discount_amount")
	private double discountAmount;

	@Field("tax_amount")
	private double taxAmount;

	@Field("total_amount")
	private double totalAmount;
	
	@Field("grand_total_amount")
	private double grandTotalAmount;

	private String description;

	@Field("sc_tax_amount")
	private double scTaxAmount;

	@Field("dc_tax_amount")
	private double dcTaxAmount;

	@Field("pc_tax_amount")
	private double pcTaxAmount;

	@Field("items")
	private List<OrderItem> orderItems;

	@Field("tax")
	private List<OrderTax> orderTax;

	@Field("discount")
	private List<OrderDiscount> orderDiscount;

	@Field("discount_type")
	private String discountType;

	private OrderStatusType status;

	@Field("delivery_charge")
	private double deliveryCharge;

	@Field("service_charge")
	private double serviceCharge;

	@Field("packaging_charge")
	private double packagingCharge;

	@Field("expected_delivery_time")
	private String expectedDeliveryTime;

	@Field("special_instructions")
	private String specialInstructions;

	@Field("delivery_details")
	private DeliveryDetails deliveryDetails;

	@Field("order_time")
	private LocalDateTime orderTime;

	@Field("created_at")
	@CreatedDate
	private LocalDateTime createdAt;

	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;

	@Field("restaurant_id")
	private String restaurantId;

	@Field("minimum_prep_time")
	private String minPrepTime;

	@Field("minimum_delivery_time")
	private String minDeliveryTime;
	
	@Field("delivery_tracking_link")
	private String deliveryTrackingLink;

	@Field("screen")
	private String screen;
	
	@Field("seat")
	private String seat;
	
	@Field("order_logs")
    private List<OrderLog> orderLogs = new ArrayList<>();

	@Field("follow_up_by")
	private String followUpBy;

	@Field("case")
	private OrderCase orderCase;

	@Field("platform_fee")
	private Double platformFee;

	@Data
	@NoArgsConstructor
	public static class OrderItem {

		@Field("item_id")
		private String id;

		@Field("item_name")
		private String name;

		@Field("item_discount")
		private double itemDiscount;

		private double price;

		@Field("final_price")
		private double finalPrice;

		private int quantity;

		@Field("variation_name")
		private String variationName;

		@Field("variation_id")
		private String variationId;

		@Field("item_tax")
		private List<OrderItemTax> orderItemTax;

		@Field("order_addon_items")
		private List<OrderAddonItem> orderAddonItems;

		@Field("item_attribute")
		private String itemAttribute;
	}

	@Data
	@NoArgsConstructor
	public static class OrderTax {

		private String id;

		private String title;

		private String type;

		private double price;

		private double tax;

		@Field("restaurant_liable_amt")
		private double restaurantLiableAmt;
	}

	@Data
	@NoArgsConstructor
	public static class OrderAddonItem {

		@Field("addon_item_id")
		private String addonItemId;

		@Field("addon_item_name")
		private String addonItemName;

		@Field("addon_group_name")
		private String addonGroupName;

		private double price;

		@Field("addon_group_id")
		private String addonGroupId;

		private int quantity;
	}

	@Data
	@NoArgsConstructor
	public static class OrderDiscount {

		private String id;

		private String title;

		private String type;

		private String price;
	}

	@Data
	@NoArgsConstructor
	public static class OrderItemTax {
		private String id;
		private String name;
		private double amount;
	}

	@Data
	@NoArgsConstructor
	public static class DeliveryDetails {
		@Field("address_id")
		private String addressId;
		private String service;
		@Field("pickup_now")
		private boolean pickupNow;
		@Field("network_id")
		private int networkId;
	}
	
	@Data
    @NoArgsConstructor
    public static class OrderLog {
        @Field("status")
        private String status;
        
        @Field("logged_at")
        private LocalDateTime loggedAt;

        public OrderLog(String status) {
            this.status = status;
            this.loggedAt = LocalDateTime.now();
        }
    }

	@Data
	@NoArgsConstructor
	public static class OrderCase {

		@Field("is_case")
		private boolean isCase;

		@Field("details")
		private String details;

		@Field("created_by")
		private String createdBy;

		@Field("updated_by")
		private String updatedBy;

		@Field("created_at")
		@CreatedDate
		private LocalDateTime createdAt = LocalDateTime.now();

		@Field("updated_at")
		@LastModifiedDate
		private LocalDateTime updatedAt;

		@Field("resolution")
		private String resolution;

		@Field("status")
		private CaseStatusType status;

	}
	
	


}
