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
public class Item extends BaseEntity {

	private static final long serialVersionUID = 1L;
	
    @Field("itemdescription")
    private String itemDescription;

    @Field("item_rank")
    private String itemRank;

    @Field("item_allow_addon")
    private String itemAllowAddon;

    @Field("variation_group_name")
    private String variationGroupName;

    private List<AddonGroup> addon;

    @Field("item_favorite")
    private String itemFavorite;

    @Field("item_tax")
    private String itemTax;

    @Field("in_stock")
    private String inStock;

    @Field("item_allow_variation")
    private String itemAllowVariation;

    private List<Variation> variation;

    @Field("item_packing_charges")
    private String itemPackingCharges;

    @Field("ignore_taxes")
    private String ignoreTaxes;

    

    private String price;

    @Field("item_order_type")
    private String itemOrderType;

    @Field("minimum_preparation_time")
    private String minimumPreparationTime;

    @Field("item_addon_based_on")
    private String itemAddonBasedOn;

    @Field("item_image_url")
    private String itemImageUrl;

    @Field("item_name")
    private String itemName;

    private List<String> cuisine;

    private String active;

    @Field("ignore_discounts")
    private String ignoreDiscounts;

    @Field("item_attribute_id")
    private String itemAttributeId;

    @Field("is_recommend")
    private String isRecommend;

    @Field("gst_type")
    private String gstType;

    @Field("item_category_id")
    private String itemCategoryId;
}

