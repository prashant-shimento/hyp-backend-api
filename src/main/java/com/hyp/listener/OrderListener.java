package com.hyp.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.hyp.entity.Order;
import com.hyp.enums.OrderStatusType;
import com.hyp.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderListener implements MessageListener {

	@Autowired
	OrderService orderService;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();
		if (expiredKey.startsWith("order:") && expiredKey.endsWith(":state")) {
			String orderId = expiredKey.split(":")[1];
			log.info("Received Order Expiry from Redis for {}", orderId);
			Order order = orderService.findById(orderId);
			if (order != null) {
				if (order.getStatus().equals(OrderStatusType.PAYMENT_PENDING)) {
					log.info("Order status updated as cancelled for {}", orderId);
					orderService.updateOrderStatus(orderId, OrderStatusType.CANCELLED);
				}
			}
		}
	}

}
