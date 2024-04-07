package com.hyp.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryOrderRequest {

	@JsonProperty("brand")
	private Brand brand;
	@JsonProperty("channel")
	private String channel;
	@JsonProperty("sender_detail")
	private ContactDetail senderDetail;
	@JsonProperty("poc_detail")
	private ContactDetail pocDetail;
	@JsonProperty("trips")
	private List<Trip> trips;

	@Data
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Brand {
		@JsonProperty("code")
		private String code;
		@JsonProperty("location_code")
		private String locationCode;
		@JsonProperty("name")
		private String name;
	}

	@Data
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Address {
		@JsonProperty("address_line_1")
		private String addressLine1;
		@JsonProperty("address_line_2")
		private String addressLine2;
		@JsonProperty("label")
		private String label;
		@JsonProperty("landmark")
		private String landmark;
		@JsonProperty("city")
		private String city;
		@JsonProperty("state")
		private String state;
		@JsonProperty("country")
		private String country;
		@JsonProperty("pincode")
		private String pincode;
		@JsonProperty("latitude")
		private double latitude;
		@JsonProperty("longitude")
		private double longitude;
		@JsonProperty("instructions_to_reach")
		private String instructionsToReach;
	}

	@Data
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ContactDetail {
		@JsonProperty("address")
		private Address address;
		@JsonProperty("name")
		private String name;
		@JsonProperty("mobile")
		private String mobile;
		@JsonProperty("email")
		private String email;
	}

	@Data
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Package {
		@JsonProperty("label")
		private String label;
		@JsonProperty("quantity")
		private int quantity;
		@JsonProperty("dead_weight")
		private int deadWeight;
		@JsonProperty("volumetric_weight")
		private int volumetricWeight;
		@JsonProperty("length")
		private int length;
		@JsonProperty("breadth")
		private int breadth;
		@JsonProperty("height")
		private int height;
	}

	@Data
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Product {
		@JsonProperty("name")
		private String name;
		@JsonProperty("sku")
		private String sku;
		@JsonProperty("price")
		private double price;
		@JsonProperty("dimension")
		private Dimension dimension;
		@JsonProperty("image_url")
		private String imageUrl;
	}

	@Data
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Dimension {
		@JsonProperty("dead_weight")
		private int deadWeight;
	}

	@Data
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Note {
		@JsonProperty("name")
		private String name;
		@JsonProperty("value")
		private String value;
	}

	@Data
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Trip {
		@JsonProperty("receiver_detail")
		private ContactDetail receiverDetail;
		@JsonProperty("packages")
		private List<Package> packages;
		@JsonProperty("source_order_id")
		private String sourceOrderId;
		@JsonProperty("reference_id")
		private String referenceId;
		@JsonProperty("cod_amount")
		private double codAmount;
		@JsonProperty("bill_amount")
		private double billAmount;
		@JsonProperty("products")
		private List<Product> products;
		@JsonProperty("notes")
		private List<Note> notes;
		@JsonProperty("delivery_date")
		private String deliveryDate;
		@JsonProperty("delivery_slot")
		private String deliverySlot;
	}
}
