package com.hyp.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.hyp.enums.FeeType;
import com.hyp.enums.PartnerType;
import com.hyp.model.PaymentRoute;
import com.hyp.model.PlatformFee;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.enums.DeliveryPartner;
import com.hyp.model.Location;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "restaurants")
public class Restaurant {

	@Id
	@Field("id")
	private String id;

	private boolean active;

	@Field("currency_html")
	private String currencyHtml;

	private String country;

	@Field("minimum_order_amount")
	private String minimumOrderAmount;

	@Field("restaurant_name")
	private String restaurantName;

	@Field("website_url")
	private String websiteUrl;

	@Field("packaging_applicable_on")
	private String packagingApplicableOn;

	private String city;

	@Field("packaging_charge")
	private String packagingCharge;

	@Field("calculate_tax_on_delivery")
	private Integer calculateTaxOnDelivery;

	@Field("packaging_charge_type")
	private String packagingChargeType;

	@Field("delivery_share_percentage")
	private Double deliverySharePercentage;

	@Field("discount_percentage")
	private Double discountPercentage;

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

	@Field("fssai")
	private String fssai;

	@Field("delivery_hours")
	private List<DeliveryHours> deliveryHours;

	@Field("calculate_tax_on_packing")
	private Integer calculateTaxOnPacking;

	@Field("delivery_charge")
	private String deliveryCharge;

	@Field("minimum_delivery_time")
	private String minimumDeliveryTime;

	@Field("turn_on_time")
	private LocalDateTime turnOnTime;

	@Field("status_reason")
	private String statusReason;

	@Field("delivery_radius")
	private Double deliveryRadius;

	@Field("delivery_partner")
	private DeliveryPartner deliveryPartner;

	@Field("support_contact")
	private String supportContact;

	@Field("created_at")
	@CreatedDate
	private LocalDateTime createdAt = LocalDateTime.now();;

	@Field("updated_at")
	@LastModifiedDate
	private LocalDateTime updatedAt;

	@Field("screens")
	private List<String> screens;

	@Field("logo_url")
	private String logoUrl;

	@Field("email")
	private String email;

	@Field("restaurant_partner")
	private String restaurantPartner;

	@Field("pos_partner")
	private String posPartner;

	@Field("payment_partner")
	private String paymentPartner;

	@Field("instant_refund")
	private boolean instantRefund;

	@Field("fulfillment_delay")
	private Integer fulfillmentDelay;

	@Field("serviceable")
	private boolean serviceable;

	@Field("serviceable_message")
	private String serviceableMessage;

	@Field("platform_fee")
	private List<PlatformFee> platformFee;

	@Field("delivery_fee")
	private Double deliveryFee;

	@Field("is_payment_routing_enabled")
	private boolean paymentRoutingEnabled;

	@Field("payment_route_list")
	private List<PaymentRoute> paymentRouteList;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DeliveryHours {
		private String from;
		private String to;
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
