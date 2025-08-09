package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.CustomerTestimonialDto;
import com.hyp.entity.CustomerTestimonial;
import com.hyp.translation.CustomerTestimonialTranslation;

@RestController
@RequestMapping("/customerTestimonial")
public class CustomerTestimonialController extends BaseController<CustomerTestimonialDto, CustomerTestimonial, String> {

	@Autowired
	public CustomerTestimonialTranslation customerTestimonialTranslation;

}
