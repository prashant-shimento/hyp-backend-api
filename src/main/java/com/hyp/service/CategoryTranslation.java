package com.hyp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.CategoryDto;
import com.hyp.entity.Category;

@Service
public class CategoryTranslation implements TranslationService<CategoryDto, Category> {

	@Override
	public Category getEntity(CategoryDto dto) {
		if (dto == null) {
			return null;
		}
		Category category = new Category();
		category.setId(dto.getCategoryId());
		category.setParentCategoryId(dto.getParentCategoryId());
		category.setCategoryImageUrl(dto.getCategoryImageUrl());
		category.setCategoryTimings(dto.getCategoryTimings());
		category.setActive(dto.getActive());
		category.setCategoryName(dto.getCategoryName());
		category.setCategoryRank(dto.getCategoryRank());
		return category;
	}

	@Override
	public CategoryDto getDto(Category entity) {
		if (entity == null) {
			return null;
		}
		CategoryDto dto = new CategoryDto();
		dto.setCategoryId(entity.getId());
		dto.setParentCategoryId(entity.getParentCategoryId());
		dto.setCategoryImageUrl(entity.getCategoryImageUrl());
		dto.setCategoryTimings(entity.getCategoryTimings());
		dto.setActive(entity.getActive());
		dto.setCategoryName(entity.getCategoryName());
		dto.setCategoryRank(entity.getCategoryRank());
		return dto;
	}

	@Override
	public List<CategoryDto> getDtoList(List<Category> entities) {
		if (entities == null) {
			return null;
		}
		List<CategoryDto> dtos = new ArrayList<>();
		for (Category entity : entities) {
			dtos.add(getDto(entity));
		}
		return dtos;
	}

	@Override
	public Category getPatchDto(Category existingEntity, CategoryDto dto) {
		if (existingEntity == null || dto == null) {
			return null;
		}
		if (dto.getCategoryId() != null) {
			existingEntity.setId(dto.getCategoryId());
		}
		if (dto.getParentCategoryId() != null) {
			existingEntity.setParentCategoryId(dto.getParentCategoryId());
		}
		if (dto.getCategoryImageUrl() != null) {
			existingEntity.setCategoryImageUrl(dto.getCategoryImageUrl());
		}
		if (dto.getCategoryTimings() != null) {
			existingEntity.setCategoryTimings(dto.getCategoryTimings());
		}
		if (dto.getActive() != null) {
			existingEntity.setActive(dto.getActive());
		}
		if (dto.getCategoryName() != null) {
			existingEntity.setCategoryName(dto.getCategoryName());
		}
		if (dto.getCategoryRank() != null) {
			existingEntity.setCategoryRank(dto.getCategoryRank());
		}
		return existingEntity;
	}
}
