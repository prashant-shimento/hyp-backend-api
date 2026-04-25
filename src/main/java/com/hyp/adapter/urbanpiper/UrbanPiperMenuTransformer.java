package com.hyp.adapter.urbanpiper;

import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import com.hyp.request.PosDataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrbanPiperMenuTransformer {

    private final CategoryTransformer categoryTransformer;
    private final TaxTransformer taxTransformer;
    private final RestaurantTransformer restaurantTransformer;
    private final OrderTypeProvider orderTypeProvider;

    public PosDataRequest transform(UrbanPiperMenuRequest request) {
        log.info("Transforming UrbanPiper menu request to PosDataRequest");
        
        PosDataRequest posDataRequest = new PosDataRequest();
        
        try {
            posDataRequest.setSuccess("1");
            posDataRequest.setRestaurants(restaurantTransformer.transform(request.getLocation()));
            posDataRequest.setOrdertypes(orderTypeProvider.getDefaultOrderTypes());
            posDataRequest.setParentcategories(new ArrayList<>());
            posDataRequest.setDiscounts(new ArrayList<>());
            posDataRequest.setAttributes(new ArrayList<>());
            
            if (request.getBillComponents() != null) {
                posDataRequest.setTaxes(taxTransformer.transform(request.getBillComponents()));
            } else {
                posDataRequest.setTaxes(new ArrayList<>());
            }
            
            if (request.getMenu() != null && request.getMenu().getCategories() != null) {
                MenuExtractionResult result = categoryTransformer.transform(request.getMenu().getCategories());
                posDataRequest.setCategories(result.getCategories());
                posDataRequest.setItems(result.getItems());
                posDataRequest.setVariations(result.getVariations());
                posDataRequest.setAddongroups(result.getAddonGroups());
            } else {
                posDataRequest.setCategories(new ArrayList<>());
                posDataRequest.setItems(new ArrayList<>());
                posDataRequest.setVariations(new ArrayList<>());
                posDataRequest.setAddongroups(new ArrayList<>());
            }
            
        } catch (Exception e) {
            log.error("Error transforming UrbanPiper request", e);
            throw new RuntimeException("Failed to transform UrbanPiper request", e);
        }
        
        return posDataRequest;
    }
}
