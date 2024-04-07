package com.hyp.entity;

import java.time.LocalDateTime;

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
@Document(collection = "addon_items")
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
    
    @Field("addon_item_selection_min")
    private String addonItemSelectionMin;
    
    @Field("addon_item_selection_max")
    private String addonItemSelectionMax;
    
    @Field("auto_turn_on_time")
    private LocalDateTime autoTurnOnTime;
}

