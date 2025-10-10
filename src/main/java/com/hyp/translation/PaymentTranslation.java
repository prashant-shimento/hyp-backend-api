package com.hyp.translation;

import com.hyp.dto.PaymentDto;
import com.hyp.entity.Payment;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PaymentTranslation extends BaseTranslationServiceImpl<PaymentDto, Payment> {

    @Override
    protected Class<PaymentDto> getDtoClass() {
        return PaymentDto.class;
    }

    @Override
    protected Class<Payment> getEntityClass() {
        return Payment.class;
    }
}
