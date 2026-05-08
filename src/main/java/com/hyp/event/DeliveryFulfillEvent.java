package com.hyp.event;

import com.hyp.entity.Delivery;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeliveryFulfillEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final Delivery delivery;

    public DeliveryFulfillEvent(Object source, Delivery delivery) {
        super(source);
        this.delivery = delivery;
    }
}
