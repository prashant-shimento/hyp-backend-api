package com.hyp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RefundDto {
    private String id;
    private String currency;
    private String paymentId;
    private String paymentOrderId;

    @NotBlank(message = "OrderId cannot be blank")
    private String orderId;

    private String status;

    @NotBlank(message = "Amount cannot be blank")
    private Double amount;

    private String speedProcessed;
    private String speedRequested;
    private String reason;
}
