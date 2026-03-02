package com.hyp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private Double amount;

    private String speedProcessed;
    private String speedRequested;
    private String reason;
}
