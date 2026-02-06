package com.hyp.service;

import com.hyp.entity.Restaurant;
import com.hyp.repository.RestaurantRepository;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RestaurantService extends BaseServiceImpl<Restaurant, String> {

    private static final String CACHE_NAME = "restaurants";

    @Autowired
    RestaurantRepository restaurantRepository;

    @Override
    protected String cacheName() {
        return CACHE_NAME;
    }

    @Override
    protected Class<Restaurant> entityType() {
        return Restaurant.class;
    }

    @Override
    protected List<String> additionalEvictionKeys(Restaurant entity) {
        if (entity.getMenuSharingCode() != null) {
            return List.of(entity.getId(), "menuCode:" + entity.getMenuSharingCode());
        }
        return Collections.emptyList();
    }

    /**
     * Get restaurant by menu sharing code using two-level cache.
     */
    public Restaurant findByMenuSharingCode(String menuSharingCode) {
        if (menuSharingCode == null) return null;

        return cacheService().getOrLoad(CACHE_NAME, "menuCode:" + menuSharingCode, Restaurant.class, () -> {
            log.debug("Loading restaurant by menuSharingCode from DB: {}", menuSharingCode);
            return restaurantRepository.findByMenuSharingCode(menuSharingCode);
        });
    }
}
