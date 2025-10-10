package com.hyp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "discounts")
public class Discount extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Field("discount_name")
    private String discountName;

    @Field("discount_type")
    private String discountType;

    @Field("bogo_buy_qty")
    private String bogoBuyQty;

    @Field("bogo_get_qty")
    private String bogoGetQty;

    @Field("bogo_type")
    private String bogoType;

    @Field("bogo_applicable_on_purchase")
    private String bogoApplicableOnPurchase;

    @Field("bogo_applicable_on_purchase_item_ids")
    private String bogoApplicableOnPurchaseItemIds;

    @Field("bogo_applicable_on")
    private String bogoApplicableOn;

    @Field("bogo_applicable_on_item_ids")
    private String bogoApplicableOnItemIds;

    @Field("bogo_item_amount_limit")
    private String bogoItemAmountLimit;

    @Field("bogo_purchase_discount")
    private String bogoPurchaseDiscount;

    @Field("bogo_applicable_on_item")
    private String bogoApplicableOnItem;

    @Field("bogo_applicable_on_purchase_item")
    private String bogoApplicableOnPurchaseItem;

    @Field("discount")
    private String discount;

    @Field("discount_order")
    private String discountOrder;

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
