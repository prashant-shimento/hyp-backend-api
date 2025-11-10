package com.hyp.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyp.entity.Restaurant.DeliveryHours;
import com.hyp.entity.Restaurant.RestaurantTax;
import com.hyp.enums.DeliveryPartner;
import com.hyp.model.Location;
import com.hyp.model.PaymentRoute;
import com.hyp.model.PlatformFee;
import com.hyp.model.PreOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestaurantDto extends BaseDto {
	private boolean active;
	private String menuSharingCode;
	private String currencyHtml;
	private String country;
	private List<String> images;
	private String restaurantName;
	private String websiteUrl;
	private String address;
	private String contact;
	private String landmark;
	private String city;
	private String state;
	private String pincode;
	private String minimumOrderAmount;
	private String minimumDeliveryTime;
	private String deliveryCharge;
	private List<DeliveryHours> deliveryHours;
	private DeliveryPartner deliveryPartner;
	private Double deliveryRadius;
	private RestaurantTax tax;
	private Integer calculateTaxOnDelivery;
	private String packagingApplicableOn;
	private String packagingCharge;
	private String packagingChargeType;
	private Double totalDeliverySharePercentage;
	private Double restaurantDeliverySharePercentage;
	private Double hyperAppsDeliverySharePercentage;
	private String status;
	private String remarks;
	private String nextAction;
	private Double discountPercentage;
	private String supportContact;
	private Location location;
	private String fssai;
	private List<String> screens;
	private String logoUrl;
	private String email;
	private String restaurantPartner;
	private String posPartner;
	private String paymentPartner;
	private String instantRefund;
	private Integer fulfillmentDelay;
	private boolean serviceable;
	private String serviceableMessage;
	private Double deliveryFee;
	private List<PlatformFee> platformFee;
	private boolean paymentRoutingEnabled;
	private List<PaymentRoute> paymentRouteList;
	private boolean showCustomMessage;
	private String customMessage;
	private String embedMapUrl;
	private String googleAnalytics;
	private String restaurantAltContact;
	private PreOrder preOrderConfig;
}
