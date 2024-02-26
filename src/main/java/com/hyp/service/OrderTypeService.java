package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.OrderType;
import com.hyp.repository.OrderTypeRepository;

@Service
public class OrderTypeService extends BaseServiceImpl<OrderType, String> {
	@Autowired
	OrderTypeRepository orderTypeRepository;
	
}
