package com.hyp.translation;

import com.hyp.dto.OfferUsageDto;
import com.hyp.entity.OfferUsage;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OfferUsageTranslation extends BaseTranslationServiceImpl<OfferUsageDto, OfferUsage> {

    @Override
    protected Class<OfferUsageDto> getDtoClass() {
        return OfferUsageDto.class;
    }

    @Override
    protected Class<OfferUsage> getEntityClass() {
        return OfferUsage.class;
    }
}
