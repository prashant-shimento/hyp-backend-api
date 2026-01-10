package com.hyp.temporal.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OrderPaymentActivities {
    String fetchPaymentStatus(String orderId);

    void processPayment(String orderId, String paymentStatus);

    String fetchOrderStatus(String orderId);

    void dropOffOrder(String orderId);
}
