package com.hyp.listener;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.event.OrderEvent;
import com.hyp.event.OrderStatusChangeEvent;
import com.hyp.service.CustomerService;
import com.hyp.service.DeliveryService;
import com.hyp.service.NotificationService;
import com.hyp.service.OrderService;
import com.hyp.service.RestaurantService;
import com.hyp.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderEventListener {

	@Autowired
	private OrderService orderService;

	@Autowired
	SimpMessagingTemplate messageTemplate;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	CustomerService customerService;

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	NotificationService notificationService;

	@Async
	@EventListener
	public void handleProcessOrder(OrderEvent event) {
		log.info("Order Event listener handleProcessOrder");
		Order order = event.getOrder();
		orderService.processOrder(order);
	}

	@Async
	@EventListener
	public void handleOrderStatusChange(OrderStatusChangeEvent event) {
		log.info("Order Event listener handleOrderStatusChange");
		List<String> templateParameters = null;
		Order order = event.getOrder();

		messageTemplate.convertAndSend("/topic/order-status", order);

		Customer customer = customerService.findById(order.getCustomerId());
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		Delivery delivery = deliveryService.findByOrderId(order.getId());
		switch (order.getStatus()) {
		case PAID:
			templateParameters = CommonUtils.buildStringList(customer.getName(), restaurant.getRestaurantName(),
					order.getId(), order.getStatus(), restaurant.getSupportContact(), restaurant.getContact());
			notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_PAID_TEMPLATE,
					templateParameters);
			break;

		case PICKED_UP:
			templateParameters = CommonUtils.buildStringList(customer.getName(), order.getId(),
					delivery.getFulfillment().getRider().getName(), delivery.getFulfillment().getRider().getMobile(),
					restaurant.getContact(), restaurant.getSupportContact());
			notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_PICKEDUP_TEMPLATE,
					templateParameters, delivery.getFulfillment().getTrackCode());
			break;
		case DELIVERED:
			templateParameters = CommonUtils.buildStringList(customer.getName(), order.getId(), restaurant.getContact(),
					restaurant.getSupportContact(), restaurant.getRestaurantName(), restaurant.getWebsiteUrl());
			notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_DELIVERED_TEMPLATE,
					templateParameters);
			break;
		case CANCELLED:
			templateParameters = CommonUtils.buildStringList(customer.getName(), order.getId(),
					restaurant.getRestaurantName());
			notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_CANCELLED_TEMPLATE,
					templateParameters);
			break;
		default:
			break;
		}

	}

}
