package com.hyp.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.entity.Tax;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@JsonInclude(Include.NON_NULL)
public class ItemDto extends BaseDto {
	private String itemDescription;
	private String itemRank;
	private String itemAllowAddon;
	private String variationGroupName;
	private List<String> addon;
	private String itemFavorite;
	private List<String> itemTax;
	private boolean inStock;
	private String itemAllowVariation;
	private List<String> variation;
	private String itemPackingCharges;
	private String ignoreTaxes;
	private String price;
	private List<String> itemOrderType;
	private String minimumPreparationTime;
	private String itemAddonBasedOn;
	private String itemImageUrl;
	private String itemName;
	private List<String> cuisine;
    private String active;
    private String ignoreDiscounts;
    private String itemAttributeId;
    private String isRecommend;
    private String gstType;
    private String itemCategoryId;
	private List<Tax> taxes;

}
