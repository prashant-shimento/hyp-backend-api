package com.hyp.entity;

import com.hyp.enums.DeliveryPartner;
import com.hyp.model.DeliveryEstimate;
import com.hyp.model.DeliveryQuote;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "delivery_quotes")
public class DeliveryQuoteRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Field("address_id")
    private String addressId;

    private List<DeliveryEstimate> estimates;

    private DeliveryPartner primary;

    private DeliveryPartner secondary;

    private DeliveryQuote.DeliveryNetworks network;

    @Indexed(expireAfter = "30m")
    @Field("expires_at")
    private LocalDateTime expiresAt;
}
