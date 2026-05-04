package com.hyp.listener;

import com.hyp.entity.Order;
import com.hyp.event.PosOrderEvent;
import com.hyp.service.PosService;
import com.hyp.service.PosServiceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PosOrderEventListener {

    private final PosServiceFactory posServiceFactory;

    @Async
    @EventListener
    public void handleProcessPosOrder(PosOrderEvent event) {
        Order order = event.getOrder();
        PosService posService = posServiceFactory.forRestaurant(order.getRestaurantId());
        log.info(
                "PosOrderEvent received for order {} restaurant {} handler {}",
                order.getId(),
                order.getRestaurantId(),
                posService.getClass().getSimpleName());
        posService.processPosOrder(order);
    }
}
