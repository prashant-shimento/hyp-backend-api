package com.hyp.service;

import com.hyp.entity.Restaurant;
import com.hyp.repository.RestaurantRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            return List.of("menuCode:" + entity.getMenuSharingCode());
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

    @Transactional
    public Optional<Restaurant> computeAndSaveSubscriptionEnd(
            String restaurantId, LocalDateTime subscriptionStart, com.hyp.enums.SubscriptionPlan plan) {

        Optional<Restaurant> opt = restaurantRepository.findById(restaurantId);
        if (opt.isEmpty()) return Optional.empty();

        Restaurant restaurant = opt.get();
        if (restaurant.getSubscription() == null) {
            restaurant.setSubscription(new Restaurant.Subscription());
        }

        Restaurant.Subscription sub = restaurant.getSubscription();
        LocalDateTime now = LocalDateTime.now();

        if (subscriptionStart != null && subscriptionStart.isBefore(now)) {
            throw new IllegalArgumentException("Subscription start must be today or a future date");
        }

        LocalDateTime baseStart;

        if (sub.getSubscriptionEnd() != null && !sub.getSubscriptionEnd().isBefore(now)) {
            baseStart = sub.getSubscriptionEnd();
        } else {
            baseStart = subscriptionStart != null ? subscriptionStart : now;
            sub.setSubscriptionStart(baseStart);
        }

        LocalDateTime end = null;
        if (plan != null) {
            switch (plan) {
                case MONTH_12:
                    end = baseStart.plusMonths(12);
                    break;
                case MONTH_6:
                    end = baseStart.plusMonths(6);
                    break;
                case MONTH_3:
                    end = baseStart.plusMonths(3);
                    break;
                case MONTH_1:
                    end = baseStart.plusMonths(1);
                    break;
                default:
            }
        }

        sub.setSubscriptionPlan(plan);
        sub.setSubscriptionEnd(end);

        if (sub.getSubscriptionStart() == null || sub.getSubscriptionEnd() == null) {
            throw new IllegalArgumentException("Invalid subscription data");
        }

        if (sub.getSubscriptionStart().isAfter(sub.getSubscriptionEnd())) {
            throw new IllegalArgumentException("Subscription start cannot be after subscription end");
        }

        if (now.isAfter(sub.getSubscriptionEnd())) {
            sub.setDaysLeftToSubscribe(0);
        } else {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    now.toLocalDate(), sub.getSubscriptionEnd().toLocalDate());
            sub.setDaysLeftToSubscribe((int) Math.max(days, 0));
        }

        Restaurant saved = restaurantRepository.save(restaurant);
        return Optional.of(saved);
    }
}
