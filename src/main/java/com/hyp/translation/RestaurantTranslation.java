package com.hyp.translation;

import com.hyp.dto.RestaurantDto;
import com.hyp.dto.RestaurantPublicDto;
import com.hyp.entity.Restaurant;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

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

    public RestaurantPublicDto getPublicDto(Restaurant restaurant) {
        return modelMapper.map(restaurant, RestaurantPublicDto.class);
    }
}
