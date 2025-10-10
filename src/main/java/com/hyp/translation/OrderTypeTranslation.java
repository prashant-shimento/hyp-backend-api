package com.hyp.translation;

import com.hyp.dto.OrderTypeDto;
import com.hyp.entity.OrderType;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OrderTypeTranslation extends BaseTranslationServiceImpl<OrderTypeDto, OrderType> {

    @Override
    protected Class<OrderTypeDto> getDtoClass() {
        return OrderTypeDto.class;
    }

    @Override
    protected Class<OrderType> getEntityClass() {
        return OrderType.class;
    }
}
