package com.hyp.entity;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.enums.DeliveryOrderStatusType;

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
@Document(collection = "delivery")
public class Delivery extends BaseEntity {

	private static final long serialVersionUID = 1L;

	private String deliveryOrderId;
	private String orderId;
	private String referenceId;
	private String channel;
	private DeliveryOrderStatusType status;
	private ContactDetail senderDetail;
	private ContactDetail pocDetail;
	private ContactDetail receiverDetail;
	private double amount;
	private int networkId;
	private boolean pickupNow;
	private String service;
	private boolean isDeliveryScheduled;
	private LocalDateTime deliveryScheduledAt;
	private String networkToken;


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
