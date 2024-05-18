package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.CategoryDto;
import com.hyp.entity.Category;
import com.hyp.service.BaseTranslationServiceImpl;

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
