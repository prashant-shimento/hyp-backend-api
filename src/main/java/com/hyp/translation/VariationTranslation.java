package com.hyp.translation;

import com.hyp.dto.VariationDto;
import com.hyp.entity.Variation;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class VariationTranslation extends BaseTranslationServiceImpl<VariationDto, Variation> {

    @Override
    protected Class<VariationDto> getDtoClass() {
        return VariationDto.class;
    }

    @Override
    protected Class<Variation> getEntityClass() {
        return Variation.class;
    }
}
