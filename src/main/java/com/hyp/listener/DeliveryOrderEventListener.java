package com.hyp.listener;

import com.hyp.constants.Constants;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.event.DeliveryEvent;
import com.hyp.event.DeliveryOrderEvent;
import com.hyp.exception.DeliveryException;
import com.hyp.service.DeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeliveryOrderEventListener {

    @Autowired
    DeliveryService deliveryService;

    @Async
    @EventListener
    public void handleProcessDeliveryOrder(DeliveryOrderEvent event) {
        log.info("Delivery Order Event listener handleProcessDeliveryOrder");
        Order order = event.getOrder();
        deliveryService.processDeliveryOrder(order);
    }

    @Async
    @EventListener
    public void handleDeliveryEvent(DeliveryEvent event) {
        log.info("Delivery Order Event listener handleDeliveryEvent for fulfilling");
        Delivery delivery = event.getDelivery();
        try {
            deliveryService.processDeliverySmartFulfill(delivery, Constants.SYSTEM);
        } catch (DeliveryException e) {
            log.error("Exception occurred on handleDeliveryEvent for {}", delivery.getDeliveryOrderId());
        }
    }
}
