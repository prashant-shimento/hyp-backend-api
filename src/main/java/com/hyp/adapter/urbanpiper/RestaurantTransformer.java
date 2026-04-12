package com.hyp.adapter.urbanpiper;

import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import com.hyp.request.PosDataRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RestaurantTransformer {

    public List<PosDataRequest.RestaurantRequest> transform(UrbanPiperMenuRequest.Location location) {
        List<PosDataRequest.RestaurantRequest> restaurants = new ArrayList<>();
        
        if (location != null) {
            PosDataRequest.RestaurantRequest restaurant = new PosDataRequest.RestaurantRequest();
            restaurant.setRestaurantid(location.getRefId());
            restaurant.setActive("1");
            restaurant.setSourceId(location.getRefId());
            restaurant.setIngestionSource("urbanpiper");
            restaurant.setPosPartner(com.hyp.enums.PosPartner.URBAN_PIPER.name());
            
            PosDataRequest.RestaurantDetails details = new PosDataRequest.RestaurantDetails();
            details.setMinimumdeliverytime(location.getMinDeliveryTime());
            details.setMenusharingcode(location.getRefId());
            
            restaurant.setDetails(details);
            restaurants.add(restaurant);
        }
        
        return restaurants;
    }
}
