package com.hyp.validation;

import com.hyp.dto.ItemDto;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.ValidationException;
import com.hyp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class ItemValidator extends BaseValidatorImpl<ItemDto> {

    @Autowired
    CategoryService categoryService;

    @Autowired
    AddonGroupService addonGroupService;

    @Autowired
    VariationService variationService;

    @Autowired
    TaxService taxService;

    @Override
    public void validate(ItemDto dto) throws ValidationException, EntityNotFoundException {

        validateRestaurant(dto.getRestaurantId());

        if (!categoryService.isExistsById(dto.getItemCategoryId())) {
            throw new EntityNotFoundException("Category", dto.getItemCategoryId());
        }

        if (dto.getItemTax() != null && !dto.getItemTax().isEmpty()) {
            for (String taxId : dto.getItemTax()) {
                if (!taxService.isExistsById(taxId)) {
                    throw new EntityNotFoundException("Tax", taxId);
                }
            }
        }

        if ("1".equals(dto.getItemAllowAddon())) {
            if (CollectionUtils.isEmpty(dto.getAddon())) {
                throw new ValidationException("Addons cannot be empty when itemAllowAddon is '1'");
            }
            for (String addonId : dto.getAddon()) {
                if (!addonGroupService.isExistsById(addonId)) {
                    throw new EntityNotFoundException("AddonGroup", addonId);
                }
            }
        }

        if ("1".equals(dto.getItemAllowVariation())) {
            if (CollectionUtils.isEmpty(dto.getVariation())) {
                throw new ValidationException("Variations cannot be empty when itemAllowVariation is '1'");
            }

            for (String variationId : dto.getVariation()) {
                if (!variationService.isExistsById(variationId)) {
                    throw new EntityNotFoundException("Variation", variationId);
                }
            }
        }

        validatePrice(dto.getPrice(), "Item");
    }
}
