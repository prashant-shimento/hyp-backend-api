package com.hyp.validation;

import com.hyp.dto.AddonItemDto;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.ValidationException;
import com.hyp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AddonItemValidator extends BaseValidatorImpl<AddonItemDto> {

    @Autowired
    AddonItemService addonItemService;

    @Override
    public void validate(AddonItemDto dto) throws ValidationException, EntityNotFoundException {

        validateRestaurant(dto.getRestaurantId());

        validatePrice(dto.getAddonItemPrice(), "Addon Item");
    }
}
