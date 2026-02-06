package com.hyp.event.redis.topic;

import com.hyp.event.EventTopic;

public enum PaymentEventTopic implements EventTopic {
    PAYMENT_EVENTS("stream:payment:events", "payment-consumer-group");

    private final String topicName;
    private final String consumerGroup;

    PaymentEventTopic(String topicName, String consumerGroup) {
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
    }

    @Override
    public String topicName() {
        return topicName;
    }

    @Override
    public String consumerGroup() {
        return consumerGroup;
    }
}
