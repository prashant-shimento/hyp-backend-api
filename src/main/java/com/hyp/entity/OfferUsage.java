package com.hyp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "offer_usage")
public class OfferUsage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Field("customer_id")
    private String customerId;

    @Field("offer_code")
    private String offerCode;

    @Field("offer_amount")
    private Double offerAmount;

    @Field("partner_id")
    private String partnerId;

    @Field("usage_count")
    private Integer usageCount;
}
