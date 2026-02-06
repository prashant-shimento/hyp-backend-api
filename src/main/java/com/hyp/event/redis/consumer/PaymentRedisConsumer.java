package com.hyp.event.redis.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.event.payment.PaymentCreateEventHandler;
import com.hyp.event.redis.topic.PaymentEventTopic;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;

@Slf4j
@Component
public class PaymentRedisConsumer extends RedisStreamConsumer {

    private final PaymentCreateEventHandler paymentCreateEventHandler;

    @Value("${payment.stream.consumer.enabled:true}")
    private boolean consumerEnabled;

    private static final String CONSUMER_NAME = "payment-consumer-1";

    public PaymentRedisConsumer(
            JedisPooled jedis, ObjectMapper mapper, PaymentCreateEventHandler paymentCreateEventHandler) {
        super(jedis, mapper);
        this.paymentCreateEventHandler = paymentCreateEventHandler;
    }

    @PostConstruct
    public void startConsumer() {
        if (!consumerEnabled) {
            log.info("Payment stream consumer is disabled");
            return;
        }
        ensureStreamAndGroup(PaymentEventTopic.PAYMENT_EVENTS);
        Thread consumerThread = new Thread(
                () -> {
                    log.info("Starting payment stream consumer");
                    consume(PaymentEventTopic.PAYMENT_EVENTS, CONSUMER_NAME, paymentCreateEventHandler);
                },
                "payment-stream-consumer");

        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("Payment stream consumer thread started");
    }
}
