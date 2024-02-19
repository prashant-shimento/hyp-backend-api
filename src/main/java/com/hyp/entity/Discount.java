package com.hyp.entity;

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
public class Discount extends BaseEntity {

    private static final long serialVersionUID = 1L;

	@Field("discount_name")
    private String discountName;

    @Field("discount_type")
    private String discountType;

    @Field("discount")
    private String discount;

    @Field("discount_order_type")
    private String discountOrderType;

    @Field("discount_applicable_on")
    private String discountApplicableOn;

    @Field("discount_days")
    private String discountDays;

    @Field("active")
    private String active;

    @Field("discount_on_total")
    private String discountOnTotal;

    @Field("discount_starts")
    private String discountStarts;

    @Field("discount_ends")
    private String discountEnds;

    @Field("discount_time_from")
    private String discountTimeFrom;

    @Field("discount_time_to")
    private String discountTimeTo;

    @Field("discount_min_amount")
    private String discountMinAmount;

    @Field("discount_max_amount")
    private String discountMaxAmount;

    @Field("discount_has_coupon")
    private String discountHasCoupon;

    @Field("discount_category_item_ids")
    private String discountCategoryItemIds;

    @Field("discount_max_limit")
    private String discountMaxLimit;
}

