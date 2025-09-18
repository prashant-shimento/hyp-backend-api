package com.hyp.temporal.activities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.service.CustomerService;
import com.hyp.service.OneSignalAlertService;
import com.hyp.service.OrderService;
import com.hyp.service.RestaurantService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderTrackActivitiesImpl implements OrderTrackActivities {
	@Autowired
	OrderService orderService;
	@Autowired
	CustomerService customerService;
	@Autowired
	RestaurantService restaurantService;
	@Autowired
	OneSignalAlertService oneSignalAlertService;

	@Override
	public String fetchOrderStatus(String orderId) {
		Order order = orderService.findById(orderId);
		return order.getStatus().name();
	}

	@Override
	public Order fetchOrder(String orderId) {
		return orderService.findById(orderId);
	}

	@Override
	public void sendOrderTrackAlert(String orderId, long finalMinutesSinceCreation, String nextExpected) {
		Order order = fetchOrder(orderId);
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		oneSignalAlertService.notifyOrderTrackDelay(orderId, restaurant.getRestaurantName(), order.getStatus().name(),
				finalMinutesSinceCreation, nextExpected);
		log.info("Sent fulfillment delay alert for {}", orderId);

	}

}
