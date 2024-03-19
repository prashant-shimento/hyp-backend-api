package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.CustomerDto;
import com.hyp.entity.Customer;
import com.hyp.translation.CustomerTranslation;

@RestController
@RequestMapping("/api/customer")
public class CustomerController extends BaseController<CustomerDto, Customer, String> {
	
	@Autowired
	public CustomerTranslation customerTranslation;
}
