package com.hyp.entity;

import java.time.LocalDateTime;
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
@Document(collection = "items")
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

    private List<String> addon;

    @Field("item_favorite")
    private String itemFavorite;

    @Field("item_tax")
    private List<String> itemTax;

    @Field("in_stock")
    private boolean inStock;

    @Field("item_allow_variation")
    private String itemAllowVariation;

    private List<String> variation;

    @Field("item_packing_charges")
    private String itemPackingCharges;

    @Field("ignore_taxes")
    private String ignoreTaxes;

    private String price;

    @Field("item_order_type")
    private List<String> itemOrderType;

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
    
    @Field("auto_turn_on_time")
    private LocalDateTime autoTurnOnTime;
    
	private transient List<Tax> taxes;
	
	@Field("item_variations")
	private transient List<Variation> itemVariations;
	
	@Field("item_addons")
	private transient List<AddonGroup> itemAddons;
}

