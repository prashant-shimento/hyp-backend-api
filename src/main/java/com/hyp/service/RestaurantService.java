package com.hyp.service;

import com.hyp.entity.Restaurant;
import com.hyp.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestaurantService extends BaseServiceImpl<Restaurant, String> {
    @Autowired
    RestaurantRepository restaurantRepository;

    public Restaurant findByMenuSharingCode(String menuSharingCode) {
        return restaurantRepository.findByMenuSharingCode(menuSharingCode);
    }
}
