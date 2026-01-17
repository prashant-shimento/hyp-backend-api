package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerReferralDto {

    private String id;

    private String customerId;

    private String restaurantId;

    private String referralCode;

    private String referralCodeId;

    private LocalDateTime referralLinkedAt;

    private LocalDateTime referralExpiresAt;

    private LocalDateTime createdAt;
}
