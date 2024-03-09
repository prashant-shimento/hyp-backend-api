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
@Document(collection = "orderitems")
public class OrderItem {

	private static final long serialVersionUID = 1L;

	@Field("item_id")
    private String itemId;
	
	@Field("item_name")
    private String itemName;

    @Field("item_discount")
    private double itemDiscount;

    private double price;

    @Field("final_price")
    private double finalPrice;

    private int quantity;

    @Field("variation_name")
    private String variationName;

    @Field("variation_id")
    private String variationId;

    @Field("order_addon_items")
    private List<OrderAddonItem> orderAddonItems;
   
}
