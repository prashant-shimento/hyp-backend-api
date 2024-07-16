package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.PartnerDto;
import com.hyp.entity.Partner;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class PartnerTranslation extends BaseTranslationServiceImpl<PartnerDto, Partner> {

	@Override
	protected Class<PartnerDto> getDtoClass() {
		return PartnerDto.class;
	}

	@Override
	protected Class<Partner> getEntityClass() {
		return Partner.class;
	}

}
