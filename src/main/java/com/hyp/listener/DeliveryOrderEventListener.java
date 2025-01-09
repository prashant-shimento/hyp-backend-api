package com.hyp.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.hyp.entity.Order;
import com.hyp.event.DeliveryOrderEvent;
import com.hyp.service.DeliveryService;
import lombok.extern.slf4j.Slf4j;

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
		deliveryService.proceesDeliveryOrder(order);
	}

}
