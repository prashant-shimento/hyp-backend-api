package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.TaxDto;
import com.hyp.entity.Tax;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class TaxTranslation extends BaseTranslationServiceImpl<TaxDto, Tax> {

	@Override
	protected Class<TaxDto> getDtoClass() {
		return TaxDto.class;
	}

	@Override
	protected Class<Tax> getEntityClass() {
		return Tax.class;
	}

}
