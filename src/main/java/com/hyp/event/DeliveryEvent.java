package com.hyp.event;

import com.hyp.entity.Delivery;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeliveryEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final Delivery delivery;

    public DeliveryEvent(Object source, Delivery delivery) {
        super(source);
        this.delivery = delivery;
    }
}
