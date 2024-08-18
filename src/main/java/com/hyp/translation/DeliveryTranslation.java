package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.DeliveryDto;
import com.hyp.entity.Delivery;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class DeliveryTranslation extends BaseTranslationServiceImpl<DeliveryDto, Delivery> {

	@Override
	protected Class<DeliveryDto> getDtoClass() {
		return DeliveryDto.class;
	}

	@Override
	protected Class<Delivery> getEntityClass() {
		
		return Delivery.class;
	}

}
