package com.hyp.entity;

import com.hyp.annotation.GenerateId;
import com.hyp.enums.DeliveryLifecycleEvent;
import com.hyp.enums.DeliveryPartner;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@Document(collection = "delivery_events")
public class DeliveryEvent {

    @Id
    @Field("id")
    @GenerateId
    private String id;

    @Field("order_id")
    private String orderId;

    @Field("delivery_id")
    private String deliveryId;

    @Field("restaurant_id")
    private String restaurantId;

    private DeliveryPartner partner;

    @Field("event_type")
    private DeliveryLifecycleEvent eventType;

    private boolean success;

    @Field("duration_ms")
    private Long durationMs; // time from previous event in this order, null for first event

    private String note;

    private Map<String, Object> metadata;

    @Field("timestamp")
    @Indexed(expireAfterSeconds = 7776000) // 90-day TTL
    private LocalDateTime timestamp;
}
