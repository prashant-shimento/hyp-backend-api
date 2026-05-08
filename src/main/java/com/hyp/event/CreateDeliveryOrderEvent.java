package com.hyp.event;

import com.hyp.entity.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CreateDeliveryOrderEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final Order order;

    public CreateDeliveryOrderEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}
