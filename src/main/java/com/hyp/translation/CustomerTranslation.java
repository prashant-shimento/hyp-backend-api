package com.hyp.translation;

import com.hyp.dto.CustomerDto;
import com.hyp.entity.Customer;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class CustomerTranslation extends BaseTranslationServiceImpl<CustomerDto, Customer> {

    @Override
    protected Class<CustomerDto> getDtoClass() {
        return CustomerDto.class;
    }

    @Override
    protected Class<Customer> getEntityClass() {
        return Customer.class;
    }
}
