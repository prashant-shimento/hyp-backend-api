package com.hyp.translation;

import com.hyp.dto.ReferralCodeDto;
import com.hyp.entity.ReferralCode;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ReferralCodeTranslation extends BaseTranslationServiceImpl<ReferralCodeDto, ReferralCode> {

    @Override
    protected Class<ReferralCodeDto> getDtoClass() {
        return ReferralCodeDto.class;
    }

    @Override
    protected Class<ReferralCode> getEntityClass() {
        return ReferralCode.class;
    }
}
