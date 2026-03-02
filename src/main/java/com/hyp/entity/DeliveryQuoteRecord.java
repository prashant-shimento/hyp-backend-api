package com.hyp.entity;

import com.hyp.model.DeliveryQuote;
import java.time.LocalDateTime;
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

    private DeliveryQuote.DeliveryNetworks network;

    @Indexed(expireAfter = "30m")
    @Field("expires_at")
    private LocalDateTime expiresAt;
}
