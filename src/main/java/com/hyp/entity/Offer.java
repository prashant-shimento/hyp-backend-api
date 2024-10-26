package com.hyp.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.annotation.GenerateId;
import com.hyp.enums.OfferStatusType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "offers")
public class Offer {

	@Id
	@GenerateId(sequenceName = "offer_sequence")
	private String id;

	@Field("title")
	private String title;

	@Field("image_url")
	private String imageUrl;

	@Field("description")
	private String description;

	@Field("coupon_code")
	private String couponCode;

	@Field("offer_status_type")
	private OfferStatusType offerStatusType;

	@Field("discount_type")
	private String discountType;

	@Field("discount_value")
	private double discountValue;

	@Field("max_discount_value")
	private double maxDiscountValue;

	@Field("max_discount")
	private double maxDiscount;

	@Field("minimum_order_value")
	private double minimumOrderValue;

	@Field("start_date")
	private LocalDateTime startDate;

	@Field("end_date")
	private LocalDateTime endDate;

	@Field("redemption_limit")
	private int redemptionLimit; // Maximum number of redemptions allowed

	@Field("redemptions_count")
	private int redemptionsCount; // Current number of times the offer has been redeemed

}
