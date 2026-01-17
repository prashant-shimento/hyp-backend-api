package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "referral_tokens")
public class ReferralToken {

    @Id
    @GenerateId()
    private String id;

    @Indexed(unique = true)
    @Field("token")
    private String token;

    @Field("referral_code_id")
    private String referralCodeId;

    @Field("referral_code")
    private String referralCode;

    @Field("restaurant_id")
    private String restaurantId;

    @Field("customer_id")
    private String customerId;

    @Field("source")
    private String source;

    @Field("expires_at")
    @Indexed
    private LocalDateTime expiresAt;

    @Field("created_at")
    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("used_order")
    private String usedOrder;
}
