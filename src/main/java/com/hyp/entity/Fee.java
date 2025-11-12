package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import com.hyp.model.FeeRule;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "fees")
public class Fee {

    @Id
    @Field("id")
    @GenerateId()
    private String id;

    @Field("active")
    private boolean active;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("restaurant_id")
    private String restaurantId;

    @Field("is_deleted")
    private boolean isDeleted = false;

    @Field("partner_id")
    private String partnerId;

    @Field("fee_rules")
    private List<FeeRule> feeRules;
}
