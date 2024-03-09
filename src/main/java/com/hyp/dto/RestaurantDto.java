package com.hyp.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantDto {
	private String restaurantId;
	private boolean active;
	private String menuSharingCode;
	private String currencyHtml;
	private String country;
	private List<String> images;
	private String restaurantName;
	private String address;
	private String contact;
	private Double latitude;
	private Double longitude;
	private String landmark;
	private String city3;
	private String state;
	private String minimumOrderAmount;
	private String minimumDeliveryTime;
	private String deliveryCharge;
	private String deliveryHoursFrom1;
	private String deliveryHoursTo1;
	private String deliveryHoursFrom2;
	private String deliveryHoursTo2;
	private String scApplicableOn;
	private String scType;
	private String scCalculateOn;
	private String scValue;
	private String taxOnSc;
	private int calculateTaxOnPacking;
	private String pcTaxesId;
	private int calculateTaxOnDelivery;
	private String dcTaxesId;
	private String packagingApplicableOn;
	private String packagingCharge;
	private String packagingChargeType;
}
