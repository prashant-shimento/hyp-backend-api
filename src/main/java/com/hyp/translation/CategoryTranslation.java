package com.hyp.translation;

import com.hyp.dto.CategoryDto;
import com.hyp.entity.Category;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CategoryTranslation extends BaseTranslationServiceImpl<CategoryDto, Category> {

    @Override
    protected Class<CategoryDto> getDtoClass() {
        return CategoryDto.class;
    }

    @Override
    protected Class<Category> getEntityClass() {
        return Category.class;
    }
}
