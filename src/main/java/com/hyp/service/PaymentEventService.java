package com.hyp.service;

import com.hyp.entity.Order;
import com.hyp.event.EventPublisher;
import com.hyp.event.payment.PaymentCreateEvent;
import com.hyp.event.redis.topic.PaymentEventTopic;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventService {

    private final EventPublisher eventPublisher;

    /**
     * Publish a payment create event for async processing.
     * Fire-and-forget - consumer will create payment in background.
     */
    public void publishPaymentCreateEvent(Order order) {
        PaymentCreateEvent event = PaymentCreateEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .restaurantId(order.getRestaurantId())
                .amount(order.getGrandTotalAmount())
                .createdAt(System.currentTimeMillis())
                .build();

        String messageId = eventPublisher.publish(PaymentEventTopic.PAYMENT_EVENTS, event);

        log.info("Published PaymentCreateEvent for orderId={}, messageId={}", order.getId(), messageId);
    }
}
