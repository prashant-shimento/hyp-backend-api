package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Address;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class AddressTranslation extends BaseTranslationServiceImpl<AddressDto, Address> {

	@Override
	protected Class<AddressDto> getDtoClass() {
		return AddressDto.class;
	}

	@Override
	protected Class<Address> getEntityClass() {
		return Address.class;
	}

}
