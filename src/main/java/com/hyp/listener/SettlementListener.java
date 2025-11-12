package com.hyp.listener;

import com.hyp.entity.Order;
import com.hyp.event.SettlementEvent;
import com.hyp.service.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SettlementListener {

    @Autowired
    SettlementService settlementService;

    @Async
    @EventListener
    public void handleSettlementEvent(SettlementEvent event) {
        log.info("Event listener handleSettlementEvent processing via Event");
        Order order = event.getOrder();
        settlementService.processSettlement(order);
    }
}
