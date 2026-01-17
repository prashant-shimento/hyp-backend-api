package com.hyp.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReferralTokenRequest {

    @NotBlank(message = "Referral code is required")
    private String referralCode;

    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;

    private String source;
}
