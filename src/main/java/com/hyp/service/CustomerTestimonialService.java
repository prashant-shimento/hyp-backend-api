package com.hyp.service;

import com.hyp.entity.CustomerTestimonial;
import com.hyp.repository.CustomerTestimonialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerTestimonialService extends BaseServiceImpl<CustomerTestimonial, String> {
    @Autowired
    CustomerTestimonialRepository customerTestimonialRepository;
}
