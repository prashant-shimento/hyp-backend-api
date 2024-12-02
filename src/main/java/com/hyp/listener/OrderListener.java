package com.hyp.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.DeliveryException;
import com.hyp.service.DeliveryService;
import com.hyp.service.OrderService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderListener implements MessageListener {

	@Autowired
	OrderService orderService;

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	StringRedisTemplate stringRedisTemplate;

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
		if (expiredKey.startsWith("order:") && expiredKey.endsWith(":fulfill")) {
			String orderId = expiredKey.split(":")[1];
			log.info("Received Order Fulfill Expiry from Redis for {}", orderId);
			Order order = orderService.findById(orderId);
			if (order != null) {
				if (order.getStatus().equals(OrderStatusType.ACCEPTED)) {
					Delivery delivery = deliveryService.findByOrderId(order.getId());
					String fulFill = stringRedisTemplate.opsForValue().get("fulfill");
					if (delivery != null && delivery.getStatus().equals(DeliveryOrderStatusType.PENDING)) {
						try {
							if (fulFill.equalsIgnoreCase("smart")) {
								deliveryService.processDeliverySmartFulfill(delivery, Constants.SYSTEM);
							} else {
								deliveryService.processDeliveryFulfill(delivery, Constants.SYSTEM);
							}
						} catch (DeliveryException e) {
							log.error("Exception occured on redis expiry delivery fulfill");
							orderService.updateOrderStatus(orderId, OrderStatusType.DELIVERY_ERROR);
						}
					}
					log.info("Order is fulfilled on redis expiry for {}", orderId);
				}
			}
		}
	}

}
