package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.OfferDto;
import com.hyp.entity.Offer;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class OfferTranslation extends BaseTranslationServiceImpl<OfferDto, Offer> {

	@Override
	protected Class<OfferDto> getDtoClass() {
		
		return OfferDto.class;
	}

	@Override
	protected Class<Offer> getEntityClass() {
		return Offer.class;
	}

}
