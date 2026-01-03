package com.hyp.temporal.activities;

import com.hyp.constants.Constants;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.DeliveryException;
import com.hyp.observability.ObservabilityContext;
import com.hyp.service.CustomerService;
import com.hyp.service.DeliveryService;
import com.hyp.service.NotificationService;
import com.hyp.service.OneSignalAlertService;
import com.hyp.service.OrderService;
import com.hyp.service.RedisService;
import com.hyp.service.RestaurantService;
import com.hyp.util.CommonUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderFulfillmentActivitiesImpl implements OrderFulfillmentActivities {

    @Autowired
    OrderService orderService;

    @Autowired
    DeliveryService deliveryService;

    @Autowired
    RedisService redisService;

    @Autowired
    CustomerService customerService;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    OneSignalAlertService oneSignalAlertService;

    @Override
    public Order fetchOrder(String orderId) {
        ObservabilityContext.setOrderId(orderId);
        try {
            return orderService.findById(orderId);
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public Delivery fetchDelivery(String orderId) {
        ObservabilityContext.setOrderId(orderId);
        try {
            return deliveryService.findByOrderId(orderId);
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public String getFulfillmentMode() {
        return redisService.getRedisData(Constants.KEY_FULFILL).orElse(Constants.KEY_SMART);
    }

    @Override
    public void fulfillDelivery(Delivery delivery, String fulfilledBy, String fulfillType) throws DeliveryException {
        ObservabilityContext.setOrderId(delivery.getOrderId());
        try {
            deliveryService.processDeliveryOrderFulfill(delivery, fulfilledBy, fulfillType);
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public void updateOrderStatus(String orderId, OrderStatusType status) {
        ObservabilityContext.setOrderId(orderId);
        try {
            orderService.updateOrderStatus(orderId, status);
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public void sendAlert(String orderId) {
        ObservabilityContext.setOrderId(orderId);
        try {
            Order order = orderService.findById(orderId);
            Customer customer = customerService.findById(order.getCustomerId());
            Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
            List<String> parameters = CommonUtils.buildStringList(
                    orderId,
                    restaurant.getRestaurantName(),
                    order.getStatus(),
                    customer.getName(),
                    customer.getMobile(),
                    "-",
                    "-");
            notificationService.sendInternalGroupNotification(Constants.META_DELIVERY_DELAY_ALERT_TEMPLATE, parameters);
            oneSignalAlertService.notifyDeliveryDelay(
                    orderId,
                    restaurant.getRestaurantName(),
                    order.getStatus(),
                    customer.getName(),
                    customer.getMobile());
            log.info("Sent fulfillment delay alert");
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public Delivery createDelivery(Order order) {
        ObservabilityContext.setOrderId(order.getId());
        try {
            deliveryService.processDeliveryOrder(order);
            return deliveryService.findByOrderId(order.getId());
        } finally {
            ObservabilityContext.clear();
        }
    }
}
