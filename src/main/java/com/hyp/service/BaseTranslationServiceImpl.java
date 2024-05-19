package com.hyp.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseTranslationServiceImpl<DTO, T> implements BaseTranslationService<DTO, T> {

	@Autowired
	protected ModelMapper modelMapper;

	@Override
	public T getEntity(DTO dto) {
		return modelMapper.map(dto, getEntityClass());
	}

	@Override
	public DTO getDto(T entity) {
		return modelMapper.map(entity, getDtoClass());
	}

	@Override
	public List<DTO> getDtoList(List<T> entities) {
		return entities.stream().map(this::getDto).collect(Collectors.toList());
	}

	@Override
	public void updateEntityFromDto(DTO dto, T entity) {
		modelMapper.map(dto, entity);
	}

	protected abstract Class<DTO> getDtoClass();

	protected abstract Class<T> getEntityClass();
}
