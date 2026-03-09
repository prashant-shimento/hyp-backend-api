package com.hyp.translation;

import com.hyp.dto.AffiliateDto;
import com.hyp.entity.Affiliate;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AffiliateTranslation extends BaseTranslationServiceImpl<AffiliateDto, Affiliate> {

    @Override
    protected Class<AffiliateDto> getDtoClass() {
        return AffiliateDto.class;
    }

    @Override
    protected Class<Affiliate> getEntityClass() {
        return Affiliate.class;
    }
}
