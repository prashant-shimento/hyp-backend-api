package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import com.hyp.model.FeeRule;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "settlements")
public class Settlement {

    @Id
    @Field("id")
    @GenerateId()
    private String id;

    @Field("order_id")
    private String orderId;

    @Field("item_total")
    private double itemTotal;

    @Field("discount")
    private double discount;

    @Field("tax")
    private double tax;

    @Field("bill")
    private double bill;

    @Field("net_bill")
    private double netBill;

    @Field("platform_fee")
    private double platformFee;

    @Field("pos_fee")
    private double posFee;

    @Field("payment_gateway_fee")
    private double paymentGatewayFee;

    @Field("total_fees")
    private double totalFees;

    @Field("delivery_charge")
    private double deliveryCharge;

    @Field("platform_delivery_share")
    private double platformDeliveryShare;

    @Field("merchant_delivery_share")
    private double merchantDeliveryShare;

    @Field("total_settlement")
    private double totalSettlement;

    @Field("partner_id")
    private String partnerId;

    @Field("settlement_status")
    private String settlementStatus;

    @Field("fee_applied")
    private List<FeeRule> feesApplied;

    @Field("computed_at")
    private Instant computedAt = Instant.now();

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("restaurant_id")
    private String restaurantId;

    @Field("order_at")
    private LocalDateTime orderAt;
}
