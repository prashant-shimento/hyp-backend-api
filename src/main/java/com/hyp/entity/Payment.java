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
@Document(collection = "payments")
public class Payment extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Field("order_id")
    private String orderId;

    @Field("payment_order_id")
    private String paymentOrderId;
    
    @Field("payment_id")
    private String paymentId;

    private String provider;

    private String status;
    
    private double amount;
    
    private String receipt;
    
    private String currency;
    
    private String signature;
    
    
    
    	
}
