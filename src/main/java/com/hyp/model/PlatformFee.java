package com.hyp.model;

import com.hyp.enums.FeeType;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFee {
    @Field("min_order_amount")
    private Double minOrderAmount;

    @Field("max_order_amount")
    private Double maxOrderAmount;

    @Field("fee_type")
    private FeeType feeType;

    @Field("fee_value")
    private Double feeValue;
}
