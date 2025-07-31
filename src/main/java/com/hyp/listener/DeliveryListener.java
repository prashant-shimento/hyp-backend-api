package com.hyp.listener;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.hyp.model.Location;
import com.hyp.model.RiderLocation;
import com.hyp.service.CustomerService;
import com.hyp.service.DeliveryService;
import com.hyp.service.LocationService;
import com.hyp.service.NotificationService;
import com.hyp.service.OneSignalAlertService;
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

	@Autowired
	private OneSignalAlertService oneSignalAlertService;

	@Value("${onesignal.app.id}")
	private String appId;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();
		handleDeliveryExpiry(expiredKey);
	}

	private void handleDeliveryExpiry(String expiredKey) {
		String[] parts = expiredKey.split(":");
		if (parts.length < 3) {
			log.warn("Invalid expiredKey format: {}", expiredKey);
			return;
		}
		if (expiredKey.startsWith("delivery:")) {
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
					oneSignalAlertService.notifyDeliveryDelay(orderId, restaurant.getRestaurantName(),
							order.getStatus(), customer.getName(), customer.getMobile());
					log.info("Sent delivery delay alert for Order ID: {}", orderId);

					String triggerFulfillOnRiderDelay = redisService
							.getRedisData(Constants.REDIS_KEY_TRIGGER_FULFILL_ON_RIDER_DELAY).orElse("false");
					log.info("triggerSmartFulfill on delay enabled status {}", triggerFulfillOnRiderDelay);
					if (triggerFulfillOnRiderDelay.equalsIgnoreCase("true")) {
						log.info("Smart fulfilling the order again due to delay in assigning rider for order, {}",
								orderId);
						try {
							deliveryService.processDeliverySmartFulfill(delivery, Constants.SYSTEM);
						} catch (DeliveryException e) {
							log.error(
									"Exception occurred on processDeliverySmartFulfill on expiry Order for {}, Exception {}",
									orderId, e.getMessage());
						}
					}

				}

			}
		}
		if (expiredKey.startsWith("rider_location:")) {
			try {
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
				Duration expiryDuration;
				DeliveryFulfillStatusType currentStatus = delivery.getFulfillment().getStatus();

				// Fetch last known rider location from logs
				Optional<Location> lastKnownLocation = delivery.getFulfillment().getLogs().stream()
						.filter(log -> log.getStatus().equals(lastStatus)).map(Log::getLocation)
						.filter(Objects::nonNull).findFirst();

				// Fetch current rider location from live tracking API
				RiderLocation currentRiderLocation = null;
				if (delivery.getFulfillment().getChannel().getName().equalsIgnoreCase("porter")
						|| delivery.getService().equalsIgnoreCase("porter")) {
					log.info("Getting Porter Rider location of the order {}", orderId);
					currentRiderLocation = deliveryService.getPorterRiderLocation(delivery.getDeliveryOrderId());
				} else {
					log.info("Getting Rider location of the order {}", orderId);
					currentRiderLocation = deliveryService.getRiderLocation(delivery.getDeliveryOrderId());
				}
				log.info("Current Rider location of the order {}, status {},Location: {}", orderId, lastStatus,
						currentRiderLocation.toString());

				boolean isLocationMissing = currentRiderLocation.getData() == null
						|| currentRiderLocation.getData().getLatitude() == null
								&& currentRiderLocation.getData().getLongitude() == null;
				if (isLocationMissing) {
					log.warn("Rider location is missing or incomplete for Order ID: {}. Setting fallback expiry.",
							orderId);
					expiryDuration = Duration.ofMinutes(2);
				} else {
					if (lastKnownLocation.isPresent()) {
						Location lastLocation = lastKnownLocation.get();
						Location currentLocation = currentRiderLocation.getData();
						Double currentLat = currentLocation.getLatitude();
						Double currentLong = currentLocation.getLongitude();

						if (Double.compare(lastLocation.getLatitude(), currentLat) == 0
								&& Double.compare(lastLocation.getLongitude(), currentLong) == 0) {

							log.warn("Rider has NOT moved for Order ID: {}", orderId);

							Rider rider = delivery.getFulfillment().getRider();
							List<String> parameters = CommonUtils.buildStringList(orderId,
									restaurant.getRestaurantName(), delivery.getFulfillment().getStatus(),
									customer.getName(), customer.getMobile(), rider != null ? rider.getName() : "-",
									rider != null ? rider.getMobile() : "-");

							notificationService.sendInternalGroupNotification(Constants.META_RIDER_DELAY_ALERT_TEMPLATE,
									parameters);
							oneSignalAlertService.notifyRiderNotMovingAlert(orderId, restaurant.getRestaurantName(),
									delivery.getFulfillment().getStatus(), customer.getName(), customer.getMobile(),
									rider != null ? rider.getName() : "-", rider != null ? rider.getMobile() : "-");
						} else {
							log.info("Rider has MOVED for Order ID: {}, updating last known location.", orderId);
						}
					}

					if (List.of(DeliveryFulfillStatusType.OUT_FOR_PICKUP, DeliveryFulfillStatusType.REACHED_PICKUP,
							DeliveryFulfillStatusType.PICKED_UP).contains(currentStatus)) {
						String riderLocationPickupStage = redisService
								.getRedisData(Constants.REDIS_KEY_RIDER_LOCATION_PICKUP_STAGE).orElse("5");
						expiryDuration = Duration.ofMinutes(Long.parseLong(riderLocationPickupStage));
					} else if (List
							.of(DeliveryFulfillStatusType.OUT_FOR_DELIVERY, DeliveryFulfillStatusType.REACHED_DELIVERY)
							.contains(currentStatus)) {
						String riderLocationOfdStage = redisService
								.getRedisData(Constants.REDIS_KEY_RIDER_LOCATION_OFD_STAGE).orElse("10");
						expiryDuration = Duration.ofMinutes(Long.parseLong(riderLocationOfdStage));
					} else {
						log.info("Order ID: {} has reached final status: {}, stopping tracking.", orderId,
								currentStatus);
						return;
					}

				}

				if (currentStatus != DeliveryFulfillStatusType.DELIVERED) {
					String locationKey = "rider_location:" + orderId + ":" + currentStatus;
					redisService.setRedisData(locationKey, currentStatus, expiryDuration.toSeconds());
				}

			} catch (DeliveryException e) {
				log.error("Error processing rider location expiry: {}", e.getMessage(), e);
			}
		}
	}

}
