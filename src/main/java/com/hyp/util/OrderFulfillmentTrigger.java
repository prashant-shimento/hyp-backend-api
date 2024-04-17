package com.hyp.util;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

import com.hyp.entity.Order;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class OrderFulfillmentTrigger implements Trigger {

	private final Order order;
	private LocalDateTime triggerTime;

	public OrderFulfillmentTrigger(Order order) {
		this.order = order;
	}

	@Override
	public Instant nextExecution(TriggerContext triggerContext) {
		LocalDateTime orderPlacementTime = order.getOrderTime();
		int minPrepTime = Integer.parseInt(order.getMinPrepTime());
		LocalDateTime nextExecutionTime = orderPlacementTime.plusMinutes(minPrepTime);
		triggerTime = nextExecutionTime;
		return nextExecutionTime.atZone(ZoneId.systemDefault()).toInstant();
	}

	public LocalDateTime getTriggerTime() {
		return triggerTime;
	}
}
