package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RazorpayVerifyDto {

    @NotBlank(message = "Razorpay payment ID cannot be blank")
    @JsonProperty("razorpay_payment_id")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay order ID cannot be blank")
    @JsonProperty("razorpay_order_id")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay signature cannot be blank")
    @JsonProperty("razorpay_signature")
    private String razorpaySignature;
}
