package com.hyp.temporal.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface DeliveryAssignmentActivities {

    int getAssignmentTimeoutMinutes(String orderId);

    void switchDeliveryProvider(String orderId, String deliveryId);
}
