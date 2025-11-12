package com.hyp.translation;

import com.hyp.dto.SettlementDto;
import com.hyp.entity.Settlement;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SettlementTranslation extends BaseTranslationServiceImpl<SettlementDto, Settlement> {

    @Override
    protected Class<SettlementDto> getDtoClass() {
        return SettlementDto.class;
    }

    @Override
    protected Class<Settlement> getEntityClass() {
        return Settlement.class;
    }
}
