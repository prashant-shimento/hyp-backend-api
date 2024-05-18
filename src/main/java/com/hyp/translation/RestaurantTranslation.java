package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.RestaurantDto;
import com.hyp.entity.Restaurant;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class RestaurantTranslation extends BaseTranslationServiceImpl<RestaurantDto, Restaurant> {

	@Override
	protected Class<RestaurantDto> getDtoClass() {
		return RestaurantDto.class;
	}

	@Override
	protected Class<Restaurant> getEntityClass() {
		return Restaurant.class;
	}

}
