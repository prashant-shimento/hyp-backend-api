package com.hyp.translation;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Address;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

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
