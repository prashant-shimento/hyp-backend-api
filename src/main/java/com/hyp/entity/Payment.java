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

	@Field("order_id")
    private String orderId;

    @Field("payment_order_id")
    private String paymentOrderId;

    private String active;

    private String attributes;
}
