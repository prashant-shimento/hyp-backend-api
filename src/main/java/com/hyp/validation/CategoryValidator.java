package com.hyp.validation;

import com.hyp.dto.CategoryDto;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.ValidationException;
import com.hyp.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CategoryValidator extends BaseValidatorImpl<CategoryDto> {

    @Autowired
    CategoryService categoryService;

    @Override
    public void validate(CategoryDto dto) throws ValidationException, EntityNotFoundException {
        validateRestaurant(dto.getRestaurantId());
    }
}
