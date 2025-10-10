package com.hyp.temporal.activities;

import com.hyp.entity.Order;
import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OrderTrackActivities {
    String fetchOrderStatus(String orderId);

    public Order fetchOrder(String orderId);

    public void sendOrderTrackAlert(String orderId, long finalMinutesSinceCreation, String nextExpected);
}
