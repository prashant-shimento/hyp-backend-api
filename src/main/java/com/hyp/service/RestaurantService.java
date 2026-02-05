package com.hyp.service;

import com.hyp.entity.Restaurant;
import com.hyp.repository.RestaurantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RestaurantService extends BaseServiceImpl<Restaurant, String> {

    private static final String CACHE_NAME = "restaurants";

    @Autowired
    RestaurantRepository restaurantRepository;

    @Autowired
    CacheService cacheService;

    /**
     * Get restaurant by ID using two-level cache.
     * L1 (Caffeine) → L2 (Redis) → MongoDB
     */
    @Override
    public Restaurant findById(String id) {
        if (id == null) return null;

        return cacheService.getOrLoad(CACHE_NAME, id, Restaurant.class, () -> {
            log.debug("Loading restaurant from DB: {}", id);
            return super.findById(id);
        });
    }

    /**
     * Get restaurant by menu sharing code using two-level cache.
     */
    public Restaurant findByMenuSharingCode(String menuSharingCode) {
        if (menuSharingCode == null) return null;

        return cacheService.getOrLoad(CACHE_NAME, "menuCode:" + menuSharingCode, Restaurant.class, () -> {
            log.debug("Loading restaurant by menuSharingCode from DB: {}", menuSharingCode);
            return restaurantRepository.findByMenuSharingCode(menuSharingCode);
        });
    }

    /**
     * Save restaurant and evict from BOTH L1 and L2 caches.
     * This ensures all pods see the updated data.
     */
    @Override
    public Restaurant save(Restaurant entity) {
        Restaurant saved = super.save(entity);

        // Evict from both caches
        cacheService.evict(CACHE_NAME, entity.getId());
        if (entity.getMenuSharingCode() != null) {
            cacheService.evict(CACHE_NAME, "menuCode:" + entity.getMenuSharingCode());
        }

        log.debug("Restaurant saved and cache evicted: {}", entity.getId());
        return saved;
    }

    /**
     * Update restaurant and evict from BOTH L1 and L2 caches.
     */
    @Override
    public Restaurant update(Restaurant entity) {
        Restaurant updated = super.update(entity);

        // Evict from both caches
        cacheService.evict(CACHE_NAME, entity.getId());
        if (entity.getMenuSharingCode() != null) {
            cacheService.evict(CACHE_NAME, "menuCode:" + entity.getMenuSharingCode());
        }

        log.debug("Restaurant updated and cache evicted: {}", entity.getId());
        return updated;
    }
}
