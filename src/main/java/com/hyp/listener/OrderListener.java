package com.hyp.listener;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.DeliveryException;
import com.hyp.service.CustomerService;
import com.hyp.service.DeliveryService;
import com.hyp.service.NotificationService;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import com.hyp.service.RedisService;
import com.hyp.service.RestaurantService;
import com.hyp.util.CommonUtils;

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

	@Autowired
	PaymentService paymentService;

	@Autowired
	CustomerService customerService;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	NotificationService notificationService;

	@Autowired
	RedisService redisService;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();
		if (expiredKey.startsWith("order:") && expiredKey.endsWith(":state")) {
			String orderId = expiredKey.split(":")[1];
			log.info("Received Order Expiry from Redis for {}", orderId);
			Order order = orderService.findById(orderId);
			if (order != null) {
				if (order.getStatus().equals(OrderStatusType.PAYMENT_PENDING)) {
					Payment payment = paymentService.findByOrderId(orderId);
					String paymentStatus = paymentService.fetchOrderStatus(payment.getPaymentOrderId());
					if (paymentStatus.equalsIgnoreCase("created")) {
						log.info("Order status updated as cancelled for {}", orderId);
						order.setStatus(OrderStatusType.CANCELLED);
						orderService.save(order);
					}

				}
			}
		}
		if (expiredKey.startsWith("order:") && expiredKey.endsWith(":payment")) {
			String orderId = expiredKey.split(":")[1];
			log.info("Received Payment Paid Order Expiry from Redis for {}", orderId);
			Order order = orderService.findById(orderId);
			if (order != null) {
				if (order.getStatus().equals(OrderStatusType.PAYMENT_PENDING)) {
					log.info("Order status updated as cancelled for {}", orderId);
					Payment payment = paymentService.findByOrderId(orderId);
					String paymentStatus = paymentService.fetchOrderStatus(payment.getPaymentOrderId());
					if (paymentStatus.equalsIgnoreCase("captured") || paymentStatus.equalsIgnoreCase("paid")) {
						orderService.updateOrderStatus(order.getId(), OrderStatusType.PAID);
						payment.setStatus(paymentStatus);
						paymentService.save(payment);
						orderService.processOrder(order);
					}
				}
			}
		}
		if (expiredKey.startsWith("order:") && expiredKey.endsWith(":fulfill")) {
			String orderId = expiredKey.split(":")[1];
			log.info("Received Order Fulfill Expiry from Redis for {}", orderId);
			Order order = orderService.findById(orderId);
			if (order != null) {
				if (order.getStatus().equals(OrderStatusType.ACCEPTED) || order.getStatus().equals(OrderStatusType.READY_FOR_DELIVERY)) {
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
						log.info("Order is fulfilled on redis expiry for {}", orderId);
					}
				}
			}
		}
		if (expiredKey.startsWith("order:") && expiredKey.endsWith(":delivery")) {
			String orderId = expiredKey.split(":")[1];
			log.info("Received Order Delivery Check Expiry from Redis for {}", orderId);
			Order order = orderService.findById(orderId);
			if (order != null) {
				if (order.getStatus().equals(OrderStatusType.ACCEPTED)) {
					Customer customer = customerService.findById(order.getCustomerId());
					Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
					Delivery delivery = deliveryService.findByOrderId(orderId);
					if (delivery.getStatus().equals(DeliveryOrderStatusType.PENDING)) {
						List<String> parameters = CommonUtils.buildStringList(orderId, restaurant.getRestaurantName(),
								order.getStatus(), customer.getName(), customer.getMobile(), "-", "-");
						notificationService.sendInternalGroupNotification(Constants.META_DELIVERY_DELAY_ALERT_TEMPLATE, parameters);
						log.info("Sent delivery delay alert for {}", orderId);
					}
				}
			}
		}
	}

}
