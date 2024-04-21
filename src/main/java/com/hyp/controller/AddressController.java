package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Address;
import com.hyp.translation.AddressTranslation;

@RestController
@RequestMapping("/address")
public class AddressController extends BaseController<AddressDto, Address, String> {

	@Autowired
	public AddressTranslation addressTranslation;
}
