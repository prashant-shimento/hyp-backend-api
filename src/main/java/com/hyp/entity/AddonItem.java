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
public class AddonItem extends BaseEntity{
    
    private static final long serialVersionUID = 1L;

	@Field("addon_item_name")
    private String addonItemName;

    @Field("addon_item_price")
    private String addonItemPrice;

    private String active;

    private String attributes;

    @Field("addon_item_rank")
    private String addonItemRank;
}

