package com.hyp.entity;

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
@Document(collection = "order_tax")
public class OrderTax {

	private static final long serialVersionUID = 1L;

    private String title;
	
    private String type;

    private double price;

    private double tax;

    @Field("restaurant_liable_amt")
    private double restaurantLiableAmt;
}
