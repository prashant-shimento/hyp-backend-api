package com.hyp.event;

public interface EventHandler<T extends Event> {

    Class<T> payloadType();

    void handle(T event) throws Exception;
}
