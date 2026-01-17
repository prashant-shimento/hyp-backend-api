package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer_referrals")
@CompoundIndex(name = "customer_restaurant_idx", def = "{'customer_id': 1, 'restaurant_id': 1}", unique = true)
public class CustomerReferral {

    @Id
    @GenerateId()
    private String id;

    @Field("customer_id")
    private String customerId;

    @Field("restaurant_id")
    private String restaurantId;

    @Field("referral_code")
    private String referralCode;

    @Field("referral_code_id")
    private String referralCodeId;

    @Field("referral_token_id")
    private String referralTokenId;

    @Field("created_at")
    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
