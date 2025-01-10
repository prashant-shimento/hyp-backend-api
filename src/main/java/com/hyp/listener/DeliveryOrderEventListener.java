package com.hyp.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.event.DeliveryEvent;
import com.hyp.event.DeliveryOrderEvent;
import com.hyp.exception.DeliveryException;
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
	
	@Async
	@EventListener
	public void handleFullfillDeliveryEvent(DeliveryEvent event) {
		log.info("Delivery Order Event listener handleFullfillDeliveryEvent");
		Delivery delivery = event.getDelivery();
		try {
			deliveryService.processDeliverySmartFulfill(delivery, Constants.SYSTEM);
		} catch (DeliveryException e) {
			log.error("Exception occured on unallocate Delivery Order for {}", delivery.getDeliveryOrderId());
		}
		
	}

}
