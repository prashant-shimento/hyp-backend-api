package com.hyp.event;

import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentEvent extends ApplicationEvent {

    private final Order order;
    private final Payment payment;
    private final String paymentStatus;

    public PaymentEvent(Object source, Order order, Payment payment, String paymentStatus) {
        super(source);
        this.order = order;
        this.payment = payment;
        this.paymentStatus = paymentStatus;
    }
}
