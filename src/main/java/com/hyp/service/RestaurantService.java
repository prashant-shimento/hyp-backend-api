package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Restaurant;
import com.hyp.repository.RestaurantRepository;

@Service
public class RestaurantService extends BaseServiceImpl<Restaurant, String> {
	@Autowired
	RestaurantRepository restaurantRepository;
	
	public Restaurant findByMenuSharingCode(String menusharingcode) {
        return restaurantRepository.findByMenuSharingCode(menusharingcode);
    }
	

}
