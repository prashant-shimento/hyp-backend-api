package com.hyp.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.hyp.enums.OrderStatusType;
import com.hyp.enums.PaymentType;

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
@Document(collection = "orders")
public class Order extends BaseEntity {

	private static final long serialVersionUID = 1L;

    @Field("customer_id")
    private String customerId;

    @Field("order_type")
    private int orderType;

    @Field("payment_type")
    private PaymentType paymentType;

    @Field("discount_amount")
    private double discountAmount;

    @Field("tax_amount")
    private double taxAmount;

    @Field("total_amount")
    private double totalAmount;
    
    private String description;

    @Field("sc_tax_amount")
    private String scTaxAmount;

    @Field("dc_tax_amount")
    private String dcTaxAmount;

    @Field("items")
    private List<OrderItem> orderItems;
    
    @Field("tax")
    private List<OrderTax> orderTax;
    
    @Field("discount")
    private List<OrderDiscount> orderDiscount;
    
    private OrderStatusType status;
    
    @Field("delivery_charge")
    private double deliveryCharge;
    
    @Field("service_charge")
    private double serviceCharge;
    
    @Field("packaging_charge")
    private double packagingCharge;
    
    @Field("expected_delivery_time")
    private String expectedDeliveryTime;
    
    @Field("special_instructions")
    private String specialInstructions;
    
    @Field("delivery_address")
    private String deliveryAddress;
    
    @Field("order_time")
    @CreatedDate
    private LocalDateTime orderTime;

}

