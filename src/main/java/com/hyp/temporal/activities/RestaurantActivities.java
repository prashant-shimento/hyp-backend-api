package com.hyp.temporal.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface RestaurantActivities {
    @ActivityMethod
    void updateStatus(String restaurantId);
}
