package com.hyp.dto;

import java.util.List;

import lombok.Data;

@Data
public class MenuItemDto {
	private String itemid;
	private String itemallowvariation;
	private String itemrank;
	private String item_categoryid;
	private String item_ordertype;
	private String item_packingcharges;
	private String itemallowaddon;
	private String itemaddonbasedon;
	private String item_favorite;
	private String ignore_taxes;
	private String ignore_discounts;
	private String in_stock;
	private List<String> cuisine;
	private String variation_groupname;
	private List<ItemVariationDto> variation;
	private List<ItemAddonDto> addon;
	private String itemname;
	private String item_attributeid;
	private String itemdescription;
	private String minimumpreparationtime;
	private String price;
	private String active;
	private String item_image_url;
	private String item_tax;
	private String gst_type;
	private NutritionDto nutrition;
}
