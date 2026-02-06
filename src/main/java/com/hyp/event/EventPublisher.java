package com.hyp.event;

public interface EventPublisher {
    String publish(EventTopic topic, Event event);
}
