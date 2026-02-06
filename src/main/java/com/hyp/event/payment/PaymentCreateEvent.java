package com.hyp.event.payment;

import com.hyp.event.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateEvent implements Event {

    private String eventId;
    private String orderId;
    private String restaurantId;
    private double amount;
    private long createdAt;

    public static final String TYPE = "PAYMENT_CREATE";

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getAggregateId() {
        return orderId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }
}
