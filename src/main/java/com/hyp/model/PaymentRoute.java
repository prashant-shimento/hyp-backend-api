package com.hyp.model;

import com.hyp.enums.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRoute {
    @Field("recipient_id")
    private String recipientId;

    @Field("recipient_type")
    private PartnerType recipientType;
}
