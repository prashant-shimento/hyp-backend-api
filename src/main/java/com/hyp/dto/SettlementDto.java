package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyp.model.FeeRule;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SettlementDto {

    private String id;

    private String orderId;

    private double itemTotal;

    private double discount;

    private double tax;

    private double bill;

    private double netBill;

    private double platformFee;

    private double posFee;

    private double paymentGatewayFee;

    private double totalFees;

    private double deliveryCharge;

    private double platformDeliveryShare;

    private double merchantDeliveryShare;

    private double totalSettlement;

    private String partnerId;

    private String settlementStatus;

    private List<FeeRule> feesApplied;

    private Instant computedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String restaurantId;
}
