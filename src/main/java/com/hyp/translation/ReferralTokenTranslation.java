package com.hyp.translation;

import com.hyp.dto.ReferralTokenDto;
import com.hyp.entity.ReferralToken;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ReferralTokenTranslation extends BaseTranslationServiceImpl<ReferralTokenDto, ReferralToken> {

    @Override
    protected Class<ReferralTokenDto> getDtoClass() {
        return ReferralTokenDto.class;
    }

    @Override
    protected Class<ReferralToken> getEntityClass() {
        return ReferralToken.class;
    }
}
