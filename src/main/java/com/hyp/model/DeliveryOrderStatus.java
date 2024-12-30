package com.hyp.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hyp.enums.DeliveryFulfillStatusType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderStatus {

	private DeliveryOrderData data;
	
	@Data
	public static class DeliveryOrderData {
		@JsonProperty("id")
		private String id;

		@JsonProperty("dd_channel")
		private DeliveryChannel ddChannel;

		@JsonProperty("reference_id")
		private String referenceId;

		@JsonProperty("bill_amount")
		private int billAmount;

		@JsonProperty("cod_amount")
		private int codAmount;

		@JsonProperty("created_at")
		private String createdAt;

		@JsonProperty("customer_detail")
		private ContactDetail customerDetail;

		@JsonProperty("sender_detail")
		private ContactDetail senderDetail;

		@JsonProperty("poc_detail")
		private ContactDetail pocDetail;

		@JsonProperty("status")
		private String status;

		@JsonProperty("updated_at")
		private String updatedAt;

		@JsonProperty("notes")
		private List<Object> notes;

		private DeliveryFulfillment fulfillment;

		@JsonProperty("owner")
		private Owner owner;

		@JsonProperty("parent_id")
		private int parentId;

	}

	@Data
	public static class DeliveryChannel {
		@JsonProperty("name")
		private String name;

		@JsonProperty("order_id")
		private String orderId;

		@JsonProperty("user")
		private User user;

	}

	@Data
	public static class User {
		@JsonProperty("id")
		private int id;

		@JsonProperty("type")
		private int type;

	}

	@Data
	public static class DeliveryFulfillment {
		private Channel channel;
		private List<Log> logs;
		private DeliveryFulfillStatusType status;
		private LogisticsInfo pickup;
		private Rider rider;
		private LogisticsInfo drop;
		private Mtg mtg;
		@JsonProperty("track_code")
		private String trackCode;
		@JsonProperty("delivery_charge")
		private double deliveryCharge;

	}

	@Data
	public static class ContactDetail {
		@JsonProperty("name")
		private String name;

		@JsonProperty("mobile")
		private String mobile;

	}

	@Data
	public static class Channel {
		private String name;
		@JsonProperty("order_id")
		private String orderId;
		private String id;
	}

	@Data
	public static class Log {
		private String timestamp;
		private String status;
		private Location location;
		private String remark;
		private Rider rider;
		private Channel channel;
		private String attemptType;
	}

	@Data
	public static class Rider {
		private String id;
		private String name;
		private String mobile;
	}

	@Data
	public static class Owner {
		@JsonProperty("id")
		private int id;

		@JsonProperty("type")
		private int type;

		@JsonProperty("name")
		private String name;
	}

	@Data
	public static class Mtg {
		@JsonProperty("trip_id")
		private int tripId;
		@JsonProperty("group_id")
		private int groupId;
		@JsonProperty("rider_id")
		private int riderId;
		@JsonProperty("bundle_id")
		private int bundleId;
		@JsonProperty("sequence_number")
		private int sequenceNumber;
	}

	@Data
	public static class LogisticsInfo {
		private String eta;
		private Location location;
		private String timestamp;
		private List<Object> proof;
	}

}
