package com.hyp.temporal.activities;

import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.DeliveryException;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OrderFulfillmentActivities {
    Order fetchOrder(String orderId);

    Delivery fetchDelivery(String orderId);

    String getFulfillmentMode();

    void fulfillDelivery(Delivery delivery, String actor, String fulfillType) throws DeliveryException;

    void updateOrderStatus(String orderId, OrderStatusType status);

    void sendAlert(String orderId);
}
