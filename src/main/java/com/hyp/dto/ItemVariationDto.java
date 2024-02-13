package com.hyp.dto;

import java.util.List;

import lombok.Data;

@Data
public class ItemVariationDto {
	private String id;
	private String variationid;
	private String name;
	private String groupname;
	private String price;
	private String active;
	private String itemPackingCharges;
	private String variationrank;
	private List<ItemAddonDto> addon;
	private Integer variationallowaddon;
}
