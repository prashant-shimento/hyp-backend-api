package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.entity.Restaurant;
import com.hyp.repository.RestaurantRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class RestaurantServiceImpl extends BaseServiceImpl<Restaurant, String,RestaurantRepository> implements RestaurantService {

	@Autowired
	private RestaurantRepository repository;

}
