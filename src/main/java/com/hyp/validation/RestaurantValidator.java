package com.hyp.validation;

import com.hyp.entity.Restaurant;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.repository.RestaurantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RestaurantValidator {

    @Autowired
    private RestaurantRepository restaurantRepository;

    /**
     * Check if restaurant exists (with caching)
     */
    @Cacheable(value = "restaurantExists", key = "#restaurantId", unless = "#result == false")
    public boolean restaurantExists(String restaurantId) {
        boolean exists = restaurantRepository.existsById(restaurantId);
        log.debug("Restaurant {} exists: {}", restaurantId, exists);
        return exists;
    }

    /**
     * Get restaurant or throw exception
     */
    @Cacheable(value = "restaurants", key = "#restaurantId")
    public Restaurant getRestaurant(String restaurantId) throws EntityNotFoundException {
        return restaurantRepository
                .findById(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant", restaurantId));
    }
}
