package com.hyp.dto;

import java.util.List;

import com.hyp.entity.Restaurant.DeliveryHours;
import com.hyp.entity.Restaurant.RestaurantTax;
import com.hyp.enums.DeliveryPartner;
import com.hyp.model.Location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
	private double deliveryRadius;
	private RestaurantTax tax;
	private int calculateTaxOnDelivery;
	private String packagingApplicableOn;
	private String packagingCharge;
	private String packagingChargeType;
	private Double deliverySharePercentage;
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

}
