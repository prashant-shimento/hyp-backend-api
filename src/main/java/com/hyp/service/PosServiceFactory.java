package com.hyp.service;

import com.hyp.entity.Restaurant;
import com.hyp.enums.PosPartner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PosServiceFactory {

    private final PosService petPoojaService;
    private final PosService urbanPiperService;
    private final RestaurantService restaurantService;

    public PosServiceFactory(
            @Qualifier("petPooja") PosService petPoojaService,
            @Qualifier("urbanPiper") PosService urbanPiperService,
            RestaurantService restaurantService) {
        this.petPoojaService = petPoojaService;
        this.urbanPiperService = urbanPiperService;
        this.restaurantService = restaurantService;
    }

    public PosService forRestaurant(String restaurantId) {
        Restaurant restaurant = restaurantService.findById(restaurantId);
        if (restaurant == null || restaurant.getPosPartner() == null) {
            log.warn("No POS partner found for restaurant {}, defaulting to PetPooja", restaurantId);
            return petPoojaService;
        }
        return forPartner(restaurant.getPosPartner());
    }

    public PosService forPartner(String posPartner) {
        try {
            PosPartner partner = PosPartner.valueOf(posPartner.toUpperCase());
            return switch (partner) {
                case URBAN_PIPER -> urbanPiperService;
                case PET_POOJA, SELF -> petPoojaService;
            };
        } catch (IllegalArgumentException e) {
            log.warn("Unknown POS partner '{}', defaulting to PetPooja", posPartner);
            return petPoojaService;
        }
    }
}
