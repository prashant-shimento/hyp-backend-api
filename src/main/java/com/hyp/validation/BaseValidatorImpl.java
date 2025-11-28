package com.hyp.validation;

import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.ValidationException;
import com.hyp.service.PartnerService;
import com.hyp.service.RestaurantService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseValidatorImpl<DTO> implements BaseValidator<DTO> {

    @Autowired
    protected RestaurantService restaurantService;

    @Autowired
    protected PartnerService partnerService;

    protected void validateRestaurant(String restaurantId) throws EntityNotFoundException, ValidationException {
        if (restaurantId == null) {
            throw new ValidationException("RestaurantId is required");
        }
        if (!restaurantService.isExistsById(restaurantId)) {
            throw new EntityNotFoundException("Restaurant", restaurantId);
        }
    }

    protected void validatePartner(String partnerId) throws EntityNotFoundException {
        if (partnerId != null && !partnerService.isExistsById(partnerId)) {
            throw new EntityNotFoundException("Partner", partnerId);
        }
    }

    protected void validatePrice(String price, String fieldName) throws ValidationException {
        if (price != null) {
            try {
                Double.parseDouble(price);
            } catch (NumberFormatException e) {
                throw new ValidationException(fieldName + " price must be a valid");
            }
        }
    }
}
