package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.entity.Restaurant;
import com.hyp.repository.RestaurantRepository;

@Service
public class RestaurantService extends BaseServiceImpl<Restaurant, String,RestaurantRepository> {

}
