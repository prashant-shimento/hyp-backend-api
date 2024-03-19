package com.hyp.translation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.AttributeDto;
import com.hyp.entity.Attribute;
import com.hyp.service.TranslationService;
import com.hyp.util.CommonUtils;

@Service
public class AttributeTranslation implements TranslationService<AttributeDto, Attribute> {

	@Override
	public Attribute getEntity(AttributeDto dto) {
		Attribute entity = new Attribute();
		entity.setId(CommonUtils.genId());
		entity.setAttribute(dto.getAttribute());
		entity.setActive(dto.getActive());
		return entity;
	}

	@Override
	public AttributeDto getDto(Attribute entity) {
		AttributeDto dto = new AttributeDto();
		dto.setAttributeId(entity.getId());
		dto.setAttribute(entity.getAttribute());
		dto.setActive(entity.getActive());
		return dto;
	}

	@Override
	public List<AttributeDto> getDtoList(List<Attribute> entities) {
		List<AttributeDto> dtoList = new ArrayList<>();
		for (Attribute entity : entities) {
			dtoList.add(getDto(entity));
		}
		return dtoList;
	}

	@Override
	public Attribute getPatchDto(Attribute existingEntity, AttributeDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

}
