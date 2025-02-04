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
@Document(collection = "variations")
public class Variation extends BaseEntity {

	private static final long serialVersionUID = 1L;

	private String name;
	
	@Field("group_name")
	private String groupName;

	private String status;

	private String price;
	private String active;

	@Field("item_packing_charges")
	private String itemPackingCharges;

	@Field("variation_rank")
	private String variationRank;

	@Field("addon_group_id")
	private List<String> addonGroupId;

	@Field("variation_allow_addon")
	private int variationAllowAddon;

	@Field("variation_id")
	private String variationId;
	
	@Field("addon_groups")
	private transient List<AddonGroup> addonGroups;
	
	@Field("auto_turn_on_time")
    private LocalDateTime autoTurnOnTime;
	
}
