package com.hyp.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrderStatus {

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

	@JsonProperty("owner")
	private Owner owner;

	@JsonProperty("parent_id")
	private int parentId;

	class DeliveryChannel {
		@JsonProperty("name")
		private String name;

		@JsonProperty("order_id")
		private String orderId;

		@JsonProperty("user")
		private User user;

	}

	class User {
		@JsonProperty("id")
		private int id;

		@JsonProperty("type")
		private int type;

	}

	class ContactDetail {
		@JsonProperty("name")
		private String name;

		@JsonProperty("mobile")
		private String mobile;

	}

	class Owner {
		@JsonProperty("id")
		private int id;

		@JsonProperty("type")
		private int type;

		@JsonProperty("name")
		private String name;

	}

}
