package com.hyp.dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto extends BaseDto {

    private String orderId;
    private String paymentOrderId;
    private String paymentId;
    private String provider;
    private String status;
    private double amount;
    private String receipt;
    private String currency;
    private String signature;
    private RefundDto refund;

    @Data
    public static class RefundDto {

        private String id;
        private double amount;
        private String currency;
        private String status;
        private String speedProcessed;
        private String speedRequested;
        private Date createdAt;
    }
}
