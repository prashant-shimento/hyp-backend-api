package com.hyp.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.hyp.entity.Order;
import com.hyp.enums.OrderStatusType;

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

}
