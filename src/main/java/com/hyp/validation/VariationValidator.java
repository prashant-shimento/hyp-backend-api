package com.hyp.validation;

import com.hyp.dto.VariationDto;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.ValidationException;
import com.hyp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class VariationValidator extends BaseValidatorImpl<VariationDto> {

    @Autowired
    private VariationService variationService;

    @Autowired
    private AddonGroupService addonGroupService;

    @Override
    public void validate(VariationDto dto) throws ValidationException, EntityNotFoundException {

        validateRestaurant(dto.getRestaurantId());

        if (dto.getVariationAllowAddon() == 1) {
            if (CollectionUtils.isEmpty(dto.getAddonGroupId())) {
                throw new ValidationException("Addon Group ID must be provided when variation allows addons");
            }
            for (String addonGroupId : dto.getAddonGroupId()) {
                if (!addonGroupService.isExistsById(addonGroupId)) {
                    throw new EntityNotFoundException("AddonGroup", addonGroupId);
                }
            }
        }

        validatePrice(dto.getPrice(), "Variation price");
    }
}
