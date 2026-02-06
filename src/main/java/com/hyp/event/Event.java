package com.hyp.event;

public interface Event {
    String getEventId();

    String getAggregateId();

    String getType();

    long getCreatedAt();
}
