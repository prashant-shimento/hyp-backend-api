package com.hyp.translation;

import com.hyp.dto.CustomerTestimonialDto;
import com.hyp.entity.CustomerTestimonial;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CustomerTestimonialTranslation
        extends BaseTranslationServiceImpl<CustomerTestimonialDto, CustomerTestimonial> {

    @Override
    protected Class<CustomerTestimonialDto> getDtoClass() {
        return CustomerTestimonialDto.class;
    }

    @Override
    protected Class<CustomerTestimonial> getEntityClass() {
        return CustomerTestimonial.class;
    }
}
