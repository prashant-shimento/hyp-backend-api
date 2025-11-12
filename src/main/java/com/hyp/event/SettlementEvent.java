package com.hyp.event;

import com.hyp.entity.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SettlementEvent extends ApplicationEvent {

    private final Order order;

    public SettlementEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}
