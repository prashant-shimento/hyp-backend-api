package com.hyp.service;

import com.hyp.constants.Constants;
import com.hyp.entity.Restaurant;
import com.hyp.enums.BroadcastType;
import com.hyp.request.PosStatusRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RiderAvailabilityMonitor {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private BroadcastService broadcastService;

    @Autowired
    private OneSignalAlertService oneSignalAlertService;

    @Autowired
    private SimpMessagingTemplate messageTemplate;

    /** Called when an order transitions INTO SEARCHING_RIDER. */
    public void onSearchingRiderEntered(String restaurantId) {
        Restaurant.RiderAvailabilityConfig config = getConfig(restaurantId);
        if (config == null) return;

        String countKey = String.format(Constants.REDIS_RIDER_SEARCH_COUNT, restaurantId);
        String stateKey = String.format(Constants.REDIS_RIDER_SEARCH_STATE, restaurantId);

        long count = redisService.incrementAndGet(countKey);
        String state = redisService.getRedisData(stateKey).orElse("NORMAL");

        log.info("RiderAvailabilityMonitor enter restaurantId={} count={} state={}", restaurantId, count, state);

        if (count >= config.getMax() && !"MAX".equals(state)) {
            Restaurant restaurant = restaurantService.findById(restaurantId);
            restaurant.setActive(false);
            restaurantService.save(restaurant);
            publishStatus(restaurantId, "0", restaurant.getMenuSharingCode());
            broadcastService.systemBroadcast(
                    restaurantId,
                    BroadcastType.ALERT,
                    "Rider availability critical — " + count + " orders searching. Restaurant temporarily paused.");
            oneSignalAlertService.notifyRiderAvailabilityAlert(
                    restaurantId, restaurant.getRestaurantName(), count, "MAX");
            redisService.setRedisString(stateKey, "MAX");

        } else if (count >= config.getThreshold() && "NORMAL".equals(state)) {
            Restaurant restaurant = restaurantService.findById(restaurantId);
            String msg = config.getAlertMessage() != null
                    ? config.getAlertMessage()
                    : "High demand — finding a rider may take longer than usual";
            restaurant.setCustomMessage(msg);
            restaurant.setShowCustomMessage(true);
            restaurantService.save(restaurant);
            broadcastService.systemBroadcast(restaurantId, BroadcastType.WARNING, msg);
            oneSignalAlertService.notifyRiderAvailabilityAlert(
                    restaurantId, restaurant.getRestaurantName(), count, "THRESHOLD");
            redisService.setRedisString(stateKey, "THRESHOLD");
        }
    }

    /** Called when an order transitions OUT OF SEARCHING_RIDER. */
    public void onSearchingRiderExited(String restaurantId) {
        Restaurant.RiderAvailabilityConfig config = getConfig(restaurantId);
        if (config == null) return;

        String countKey = String.format(Constants.REDIS_RIDER_SEARCH_COUNT, restaurantId);
        String stateKey = String.format(Constants.REDIS_RIDER_SEARCH_STATE, restaurantId);

        long count = redisService.decrementAndGet(countKey);
        if (count < 0) {
            redisService.setRedisString(countKey, "0");
            count = 0;
        }
        String state = redisService.getRedisData(stateKey).orElse("NORMAL");

        log.info("RiderAvailabilityMonitor exit restaurantId={} count={} state={}", restaurantId, count, state);

        if (count < config.getThreshold() && !"NORMAL".equals(state)) {
            Restaurant restaurant = restaurantService.findById(restaurantId);
            restaurant.setCustomMessage(null);
            restaurant.setShowCustomMessage(false);
            if ("MAX".equals(state)) {
                restaurant.setActive(true);
                restaurantService.save(restaurant);
                publishStatus(restaurantId, "1", restaurant.getMenuSharingCode());
            } else {
                restaurantService.save(restaurant);
            }
            broadcastService.systemBroadcast(
                    restaurantId, BroadcastType.INFO, "Rider availability restored — orders being assigned normally.");
            redisService.removeRedisData(stateKey);
        }
    }

    private void publishStatus(String restaurantId, String storeStatus, String menuSharingCode) {
        PosStatusRequest update = new PosStatusRequest();
        update.setRestaurantId(restaurantId);
        update.setStoreStatus(storeStatus);
        update.setMenuSharingCode(menuSharingCode);
        messageTemplate.convertAndSend("/topic/restaurant-status", update);
    }

    private Restaurant.RiderAvailabilityConfig getConfig(String restaurantId) {
        try {
            Restaurant restaurant = restaurantService.findById(restaurantId);
            Restaurant.RiderAvailabilityConfig config = restaurant.getRiderAvailabilityConfig();
            if (config == null || !config.isEnabled()) return null;
            return config;
        } catch (Exception e) {
            log.error("RiderAvailabilityMonitor: failed to load config restaurantId={}", restaurantId, e);
            return null;
        }
    }
}
