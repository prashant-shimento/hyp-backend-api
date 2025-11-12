package com.hyp.translation;

import com.hyp.dto.FeeDto;
import com.hyp.entity.Fee;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FeeTranslation extends BaseTranslationServiceImpl<FeeDto, Fee> {

    @Override
    protected Class<FeeDto> getDtoClass() {
        return FeeDto.class;
    }

    @Override
    protected Class<Fee> getEntityClass() {
        return Fee.class;
    }
}
