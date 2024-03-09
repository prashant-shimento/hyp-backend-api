package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Address;
import com.hyp.repository.AddressRepository;

@Service
public class AddressService extends BaseServiceImpl<Address, String> {

	@Autowired
	public AddressRepository addressRepository;
}
