package com.hyp.validation;

import com.hyp.dto.AddonGroupDto;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.ValidationException;
import com.hyp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AddonGroupValidator extends BaseValidatorImpl<AddonGroupDto> {

    @Autowired
    AddonItemService addonItemService;

    @Override
    public void validate(AddonGroupDto dto) throws EntityNotFoundException, ValidationException {

        validateRestaurant(dto.getRestaurantId());

        if (dto.getAddonGroupItems() != null && !dto.getAddonGroupItems().isEmpty()) {
            for (String addonItemId : dto.getAddonGroupItems()) {
                if (!addonItemService.isExistsById(addonItemId)) {
                    throw new EntityNotFoundException("AddonItem", addonItemId);
                }
            }
        }
    }
}
