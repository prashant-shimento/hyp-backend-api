package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.CustomerTestimonial;
import com.hyp.repository.CustomerTestimonialRepository;

@Service
public class CustomerTestimonialService extends BaseServiceImpl<CustomerTestimonial, String> {
	@Autowired
	CustomerTestimonialRepository customerTestimonialRepository;
}
