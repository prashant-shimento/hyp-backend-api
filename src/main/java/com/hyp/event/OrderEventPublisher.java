package com.hyp.event;

import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public void publishProcessOrderEvent(Order order) {
        OrderEvent event = new OrderEvent(this, order);
        applicationEventPublisher.publishEvent(event);
    }

    public void publishOrderStatusChangeEvent(Order order) {
        OrderStatusChangeEvent event = new OrderStatusChangeEvent(this, order);
        applicationEventPublisher.publishEvent(event);
    }

    public void publishPosOrderEvent(Order order) {
        PosOrderEvent event = new PosOrderEvent(this, order);
        applicationEventPublisher.publishEvent(event);
    }

    public void publishCreateDeliveryOrderEvent(Order order) {
        CreateDeliveryOrderEvent event = new CreateDeliveryOrderEvent(this, order);
        applicationEventPublisher.publishEvent(event);
    }

    public void publishDeliveryFulfillEvent(Delivery delivery) {
        DeliveryFulfillEvent event = new DeliveryFulfillEvent(this, delivery);
        applicationEventPublisher.publishEvent(event);
    }

    public void publishSettlementEvent(Order order) {
        SettlementEvent event = new SettlementEvent(this, order);
        applicationEventPublisher.publishEvent(event);
    }

    public void publishPaymentSuccessEvent(Order order, Payment payment, String paymentStatus) {
        PaymentEvent event = new PaymentEvent(this, order, payment, paymentStatus);
        applicationEventPublisher.publishEvent(event);
    }
}
