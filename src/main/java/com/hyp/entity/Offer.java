package com.hyp.entity;

import com.hyp.enums.OfferType;
import java.time.LocalDateTime;
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
@Document(collection = "offers")
public class Offer extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Field("offer_code")
    private String offerCode;

    @Field("offer_type")
    private OfferType offerType;

    @Field("discount_value")
    private Double discountValue;

    @Field("start_date")
    private LocalDateTime startDate;

    @Field("end_date")
    private LocalDateTime endDate;

    @Field("maximum_redemption_limit")
    private String maximumRedemptionLimit;

    @Field("is_active")
    private Boolean isActive;

    @Field("partner_id")
    private String partnerId;

    @Field("notes")
    private String notes;
}
