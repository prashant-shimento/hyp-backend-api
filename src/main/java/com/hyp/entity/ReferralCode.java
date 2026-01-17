package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "referral_codes")
public class ReferralCode {

    @Id
    @GenerateId()
    private String id;

    @Indexed(unique = true)
    @Field("code")
    private String code;

    @Field("referrer_name")
    private String referrerName;

    @Field("active")
    @Builder.Default
    private boolean active = true;

    @Field("attribution_window_days")
    @Builder.Default
    private Integer attributionWindowDays = 7;

    @Field("max_attributed_orders")
    private Integer maxAttributedOrders;

    @Field("partner_id")
    private String partnerId;

    @Field("created_at")
    @CreatedDate
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;
}
