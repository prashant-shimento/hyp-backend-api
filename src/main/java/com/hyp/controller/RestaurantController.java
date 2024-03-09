package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.RestaurantDto;
import com.hyp.entity.Restaurant;
import com.hyp.service.RestaurantTranslation;

@RestController
@RequestMapping("/api/restaurant")
public class RestaurantController extends BaseController<RestaurantDto, Restaurant, String> {

	@Autowired
	public RestaurantTranslation restaurantTranslation;
}
