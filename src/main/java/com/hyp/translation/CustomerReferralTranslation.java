package com.hyp.translation;

import com.hyp.dto.CustomerReferralDto;
import com.hyp.entity.CustomerReferral;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CustomerReferralTranslation extends BaseTranslationServiceImpl<CustomerReferralDto, CustomerReferral> {

    @Override
    protected Class<CustomerReferralDto> getDtoClass() {
        return CustomerReferralDto.class;
    }

    @Override
    protected Class<CustomerReferral> getEntityClass() {
        return CustomerReferral.class;
    }
}
