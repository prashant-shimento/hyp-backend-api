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
public class Variation extends BaseEntity {

    private static final long serialVersionUID = 1L;

	private String name;

    @Field("group_name")
    private String groupName;

    private String status;
    
    @Field("variation_id")
    private String variationId;
    
    private String price;
    private String active;
    
    @Field("item_packing_charges")
    private String itemPackingCharges;
    
    @Field("variation_rank")
    private String variationRank;
    
    private List<AddonGroup> addon;
    
    @Field("variation_allow_addon")
    private int variationAllowAddon;
    
}

