package com.hyp.entity;

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
@Document(collection = "orderaddonitems")
public class OrderAddonItem {

	private static final long serialVersionUID = 1L;

	@Field("addon_item_id")
    private String addonItemId;
	
	@Field("addon_item_name")
    private String addonItemName;

    @Field("addon_group_name")
    private String addonGroupName;

    private double price;

    @Field("addon_group_id")
    private String addonGroupId;

    private int quantity;
}
