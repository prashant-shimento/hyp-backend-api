package com.hyp.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "restaurants")
public class Restaurant extends BaseEntity {

	private static final long serialVersionUID = 1L;

	

	private boolean active;

	@Field("currency_html")
	private String currencyHtml;

	private String country;

	@Field("minimum_order_amount")
	private String minimumOrderAmount;

	@Field("restaurant_name")
	private String restaurantName;

	@Field("packaging_applicable_on")
	private String packagingApplicableOn;

	private String city;

	@Field("packaging_charge")
	private String packagingCharge;

	@Field("calculate_tax_on_delivery")
	private int calculateTaxOnDelivery;

	@Field("packaging_charge_type")
	private String packagingChargeType;

	private String contact;

	private String state;

	private String landmark;
	
	private String pincode;

	private Location location;

	private List<String> images;

	private String address;

	@Field("tax")
	private RestaurantTax tax;

	@Field("menu_sharing_code")
	private String menuSharingCode;

	@Field("delivery_hours")
	private List<DeliveryHours> deliveryHours;

	@Field("calculate_tax_on_packing")
	private int calculateTaxOnPacking;

	@Field("deliverycharge")
	private String deliveryCharge;

	@Field("minimum_delivery_time")
	private String minimumDeliveryTime;

	@Field("turn_on_time")
	private LocalDateTime turnOnTime;

	@Field("status_reason")
	private String statusReason;
	
	@Field("delivery_radius")
	private double deliveryRadius;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DeliveryHours {
		private LocalTime from;
		private LocalTime to;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Location {
		private double latitude;
		private double longitude;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class RestaurantTax {
		private String dcTaxesId;
		private String pcTaxesId;
	}

}
