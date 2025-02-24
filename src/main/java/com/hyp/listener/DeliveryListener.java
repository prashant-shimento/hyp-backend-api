package com.hyp.listener;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryOrderStatus.Log;
import com.hyp.model.DeliveryOrderStatus.Rider;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.model.Location;
import com.hyp.service.CustomerService;
import com.hyp.service.DeliveryService;
import com.hyp.service.LocationService;
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

	@Autowired
	LocationService locationService;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();
		handleDeliveryExpiry(expiredKey);
	}

	private void handleDeliveryExpiry(String expiredKey) {
		if (expiredKey.startsWith("delivery:")) {
			String[] parts = expiredKey.split(":");
			if (parts.length < 3) {
				log.warn("Invalid expiredKey format: {}", expiredKey);
				return;
			}
			String orderId = parts[1];
			String status = parts[2];

			if (List.of(DeliveryFulfillStatusType.CREATED.name(), DeliveryFulfillStatusType.OUT_FOR_PICKUP.name())
					.contains(status)) {
				log.info("Received Delivery Delay Expiry from Redis for Order ID: {}", orderId);

				Order order = orderService.findById(orderId);
				if (order == null) {
					log.warn("Order not found for ID: {}", orderId);
					return;
				}

				Customer customer = customerService.findById(order.getCustomerId());
				Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
				Delivery delivery = deliveryService.findByOrderId(orderId);

				if (delivery == null || delivery.getFulfillment() == null) {
					log.warn("No fulfillment data for Order ID: {}", orderId);
					return;
				}

				DeliveryFulfillStatusType currentStatus = delivery.getFulfillment().getStatus();
				if (currentStatus.name().equals(status)) {
					Rider rider = delivery.getFulfillment().getRider();
					List<String> parameters = CommonUtils.buildStringList(orderId,
							restaurant != null ? restaurant.getRestaurantName() : "-", order.getStatus(),
							customer != null ? customer.getName() : "-", customer != null ? customer.getMobile() : "-",
							rider != null ? rider.getName() : "-", rider != null ? rider.getMobile() : "-");

					notificationService.sendInternalGroupNotification(Constants.META_DELIVERY_DELAY_ALERT_TEMPLATE,
							parameters);
					log.info("Sent delivery delay alert for Order ID: {}", orderId);
				}
			}
		}
		if (expiredKey.startsWith("rider_location:")) {
			try {
				String[] parts = expiredKey.split(":");
				if (parts.length < 3) {
					log.warn("Invalid expiredKey format: {}", expiredKey);
					return;
				}

				String orderId = parts[1];
				String lastStatus = parts[2]; // OUT_FOR_PICKUP, REACHED_PICKUP, PICKED_UP, etc.

				log.info("Received Rider Location Expiry from Redis for Order ID: {} with Status: {}", orderId,
						lastStatus);
				Order order = orderService.findById(orderId);
				Delivery delivery = deliveryService.findByOrderId(orderId);
				Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
				Customer customer = customerService.findById(order.getCustomerId());
				if (delivery == null || delivery.getFulfillment() == null) {
					log.warn("No fulfillment data found for Order ID: {}", orderId);
					return;
				}

				// Fetch last known rider location from logs
				Optional<Location> lastKnownLocation = delivery.getFulfillment().getLogs().stream()
						.filter(log -> log.getStatus().equals(lastStatus)).map(Log::getLocation)
						.filter(Objects::nonNull).findFirst();

				// Fetch current rider location from live tracking API
				DeliveryRiderLocation currentRiderLocation = deliveryService
						.getDeliveryRiderLocation(delivery.getDeliveryOrderId());

				if (lastKnownLocation.isPresent() && currentRiderLocation != null && currentRiderLocation.getLocation() != null) {
					Location lastLocation = lastKnownLocation.get();
					Location currentLocation = currentRiderLocation.getLocation();

					if (Double.compare(lastLocation.getLatitude(), currentLocation.getLatitude()) == 0
							&& Double.compare(lastLocation.getLongitude(), currentLocation.getLongitude()) == 0) {

						log.warn("Rider has NOT moved for Order ID: {}", orderId);

						Rider rider = delivery.getFulfillment().getRider();
						List<String> parameters = CommonUtils.buildStringList(orderId, restaurant.getRestaurantName(),
								delivery.getFulfillment().getStatus(), customer.getName(), customer.getMobile(),
								rider != null ? rider.getName() : "-", rider != null ? rider.getMobile() : "-");
						notificationService.sendInternalGroupNotification(Constants.META_RIDER_DELAY_ALERT_TEMPLATE,
								parameters);
					} else {
						log.info("Rider has MOVED for Order ID: {}, updating last known location.", orderId);
					}
				}

				Duration expiryDuration;
				DeliveryFulfillStatusType currentStatus = delivery.getFulfillment().getStatus();
				if (List.of(DeliveryFulfillStatusType.OUT_FOR_PICKUP, DeliveryFulfillStatusType.REACHED_PICKUP,
						DeliveryFulfillStatusType.PICKED_UP).contains(currentStatus)) {
					expiryDuration = Duration.ofMinutes(5); // Poll every 5 min
				} else if (List
						.of(DeliveryFulfillStatusType.OUT_FOR_DELIVERY, DeliveryFulfillStatusType.REACHED_DELIVERY)
						.contains(currentStatus)) {
					expiryDuration = Duration.ofMinutes(10); // Poll every 10 min
				} else {
					log.info("Order ID: {} has reached final status: {}, stopping tracking.", orderId, currentStatus);
					return;
				}

				String locationKey = "rider_location:" + orderId + ":" + currentStatus;
				redisService.setRedisData(locationKey, currentStatus, expiryDuration.toSeconds());

			} catch (DeliveryException e) {
				log.error("Error processing rider location expiry: {}", e.getMessage(), e);
			}
		}
	}

}
