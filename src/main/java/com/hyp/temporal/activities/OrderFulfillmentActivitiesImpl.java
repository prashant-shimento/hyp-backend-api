package com.hyp.temporal.activities;

import com.hyp.constants.Constants;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.DeliveryException;
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
        return orderService.findById(orderId);
    }

    @Override
    public Delivery fetchDelivery(String orderId) {
        return deliveryService.findByOrderId(orderId);
    }

    @Override
    public String getFulfillmentMode() {
        return String.valueOf(redisService.getRedisData(Constants.KEY_FULFILL));
    }

    @Override
    public void fulfillDelivery(Delivery delivery, String fulfilledBy, String fulfillType) throws DeliveryException {
        deliveryService.processDeliveryOrderFulfill(delivery, fulfilledBy, fulfillType);
    }

    @Override
    public void updateOrderStatus(String orderId, OrderStatusType status) {
        orderService.updateOrderStatus(orderId, status);
    }

    @Override
    public void sendAlert(String orderId) {
        Order order = fetchOrder(orderId);
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
                orderId, restaurant.getRestaurantName(), order.getStatus(), customer.getName(), customer.getMobile());
        log.info("Sent fulfillment delay alert for {}", orderId);
    }

    @Override
    public Delivery createDelivery(Order order) {
        deliveryService.processDeliveryOrder(order);
        return deliveryService.findByOrderId(order.getId());
    }
}
