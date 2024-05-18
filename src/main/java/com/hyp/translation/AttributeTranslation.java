package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.AttributeDto;
import com.hyp.entity.Attribute;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class AttributeTranslation extends BaseTranslationServiceImpl<AttributeDto, Attribute> {

	@Override
	protected Class<AttributeDto> getDtoClass() {
		return AttributeDto.class;
	}

	@Override
	protected Class<Attribute> getEntityClass() {
		return Attribute.class;
	}

}
