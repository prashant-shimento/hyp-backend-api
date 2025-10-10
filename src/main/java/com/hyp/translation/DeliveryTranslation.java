package com.hyp.translation;

import com.hyp.dto.DeliveryDto;
import com.hyp.entity.Delivery;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class DeliveryTranslation extends BaseTranslationServiceImpl<DeliveryDto, Delivery> {

    @Override
    protected Class<DeliveryDto> getDtoClass() {
        return DeliveryDto.class;
    }

    @Override
    protected Class<Delivery> getEntityClass() {

        return Delivery.class;
    }
}
