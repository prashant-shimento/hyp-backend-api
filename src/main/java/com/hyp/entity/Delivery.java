package com.hyp.entity;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.List;

import com.hyp.model.DeliveryOrderStatus;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;

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
@Document(collection = "deliveries")
public class Delivery extends BaseEntity {

	private static final long serialVersionUID = 1L;
	
	@Field("delivery_order_id")
	private String deliveryOrderId;
	@Field("order_id")
	private String orderId;
	@Field("reference_id")
	private String referenceId;
	private String channel;
	private DeliveryOrderStatusType status;
	@Field("sender_detail")
	private ContactDetail senderDetail;
	@Field("poc_detail")
	private ContactDetail pocDetail;
	@Field("receiver_detail")
	private ContactDetail receiverDetail;
	private double amount;
	@Field("network_id")
	private int networkId;
	@Field("pickup_now")
	private boolean pickupNow;
	private String service;
	@Field("network_token")
	private String networkToken;
	private DeliveryFulfillment fulfillment;
	@Field("fulfillment_type")
	private String fulfillmentType;
	@Field("fulfillment_at")
	private LocalDateTime fulfillmentAt;
	@Field("fulfillment_histories")
	private List<DeliveryOrderStatus.FulfillmentHistory> fulfillmentHistory;

	@Data
	@NoArgsConstructor
	public static class ContactDetail {

		@Field("address_details")
		private Address address;
		private String name;
		private String mobile;
		private String email;
	}

	@Data
	@NoArgsConstructor
	public static class Address {
		@Field("address_line_1")
		private String addressLine1;
		@Field("address_line_2")
		private String addressLine2;
		private String label;
		private String landmark;
		private String city;
		private String state;
		private String country;
		private String pincode;
		private double latitude;
		private double longitude;
		@Field("instructions_to_reach")
		private String instructionsToReach;
	}
}
