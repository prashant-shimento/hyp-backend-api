package com.hyp.listener;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.model.DeliveryOrderStatus.Rider;
import com.hyp.service.CustomerService;
import com.hyp.service.DeliveryService;
import com.hyp.service.NotificationService;
import com.hyp.service.OrderService;
import com.hyp.service.RedisService;
import com.hyp.service.RestaurantService;
import com.hyp.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DeliveryListener implements MessageListener {

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	OrderService orderService;

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
		handleDeliveryExpiry(expiredKey, DeliveryFulfillStatusType.CREATED);
		handleDeliveryExpiry(expiredKey, DeliveryFulfillStatusType.OUT_FOR_PICKUP);

	}

	private void handleDeliveryExpiry(String expiredKey, DeliveryFulfillStatusType statusType) {
		if (expiredKey.startsWith("delivery:") && expiredKey.endsWith(":" + statusType.name())) {
			String orderId = expiredKey.split(":")[1];
			log.info("Received Delivery Delay Expiry from Redis for {}", orderId);
			Order order = orderService.findById(orderId);
			Customer customer = customerService.findById(order.getCustomerId());
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			Delivery delivery = deliveryService.findByOrderId(orderId);
			if (delivery.getFulfillment().getStatus().equals(statusType)) {
				Rider rider = delivery.getFulfillment().getRider();
				List<String> parameters = CommonUtils.buildStringList(orderId, restaurant.getRestaurantName(),
						order.getStatus(), customer.getName(), customer.getMobile(),
						rider != null ? rider.getName() : "-", rider != null ? rider.getMobile() : "-");
				notificationService.sendInternalGroupNotification(Constants.META_DELIVERY_DELAY_ALERT_TEMPLATE, parameters);
				log.info("Sent delivery delay alert for {}", orderId);
			}
		}
	}

}
