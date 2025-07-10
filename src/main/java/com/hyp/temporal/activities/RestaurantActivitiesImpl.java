package com.hyp.temporal.activities;

import com.hyp.entity.Restaurant;
import com.hyp.service.RestaurantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RestaurantActivitiesImpl implements RestaurantActivities {

    @Autowired
    RestaurantService restaurantService;

    @Override
    public void updateStatus(String restaurantId) {
        Restaurant restaurant = restaurantService.findById(restaurantId);
        restaurant.setActive(true);
        restaurantService.update(restaurant);
    }
}
