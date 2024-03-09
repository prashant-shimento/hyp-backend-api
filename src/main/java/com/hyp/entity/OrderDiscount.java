package com.hyp.entity;

import org.springframework.data.mongodb.core.mapping.Document;
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
@Document(collection = "order_discount")
public class OrderDiscount {

	private static final long serialVersionUID = 1L;

    private String title;
	
    private String type;

    private String price;
}
