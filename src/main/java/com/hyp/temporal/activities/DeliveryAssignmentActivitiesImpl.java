package com.hyp.temporal.activities;

import com.hyp.delivery.DeliveryOrchestrator;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.observability.ObservabilityContext;
import com.hyp.service.DeliveryService;
import com.hyp.service.OrderService;
import com.hyp.service.RestaurantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeliveryAssignmentActivitiesImpl implements DeliveryAssignmentActivities {

    @Lazy
    @Autowired
    private DeliveryService deliveryService;

    @Lazy
    @Autowired
    private OrderService orderService;

    @Autowired
    private RestaurantService restaurantService;

    @Lazy
    @Autowired
    private DeliveryOrchestrator deliveryOrchestrator;

    /**
     * Resolves assignment SLA from the restaurant's delivery config.
     * Falls back to 8 minutes if config is missing or lookup fails.
     */
    @Override
    public int getAssignmentTimeoutMinutes(String orderId) {
        try {
            Order order = orderService.findById(orderId);
            if (order != null) {
                Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
                if (restaurant != null) {
                    return deliveryOrchestrator.getAssignmentSlaMinutes(restaurant);
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve assignment SLA for orderId={}, using default 8 min", orderId);
        }
        return 8;
    }

    @Override
    public void switchDeliveryProvider(String orderId, String deliveryId) {
        ObservabilityContext.setOrderId(orderId);
        try {
            log.info("Activity: switching delivery provider orderId={} deliveryId={}", orderId, deliveryId);
            deliveryService.switchDeliveryProvider(orderId, deliveryId);
        } catch (Exception e) {
            log.error("Failed to switch delivery provider orderId={}: {}", orderId, e.getMessage(), e);
        } finally {
            ObservabilityContext.clear();
        }
    }
}
