package com.hyp.entity;

import java.util.List;

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
public class Restaurant extends BaseEntity {

	private static final long serialVersionUID = 1L;
    
	private String active;
    
    @Field("currency_html")
    private String currencyHtml;

    private String country;

    @Field("minimumorderamount")
    private String minimumOrderAmount;

    @Field("restaurantname")
    private String restaurantName;

    @Field("packaging_applicable_on")
    private String packagingApplicableOn;

    private String city;

    private String latitude;

    @Field("packaging_charge")
    private String packagingCharge;

    @Field("calculatetaxondelivery")
    private int calculateTaxOnDelivery;

    @Field("packaging_charge_type")
    private String packagingChargeType;

    private String contact;

    private String state;

    private String landmark;

    private String longitude;

    private List<String> images;

    private String address;

    @Field("pc_taxes_id")
    private String pcTaxesId;

    @Field("deliveryhoursfrom2")
    private String deliveryHoursFrom2;

    @Field("menusharingcode")
    private String menuSharingCode;

    @Field("deliveryhoursfrom1")
    private String deliveryHoursFrom1;

    @Field("deliveryhoursto2")
    private String deliveryHoursTo2;

    @Field("deliveryhoursto1")
    private String deliveryHoursTo1;

    @Field("calculatetaxonpacking")
    private int calculateTaxOnPacking;

    @Field("dc_taxes_id")
    private String dcTaxesId;

    @Field("deliverycharge")
    private String deliveryCharge;

    @Field("minimumdeliverytime")
    private String minimumDeliveryTime;
}

