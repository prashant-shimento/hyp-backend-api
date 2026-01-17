package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReferralTokenDto {

    private String id;

    private String token;

    private String referralCodeId;

    private String referralCode;

    private String restaurantId;

    private String customerId;

    private String source;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
