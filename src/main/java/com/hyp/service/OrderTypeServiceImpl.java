package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.OrderType;
import com.hyp.repository.OrderTypeRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class OrderTypeServiceImpl extends BaseServiceImpl<OrderType, String, OrderTypeRepository> implements OrderTypeService {
	
	@Autowired
	private OrderTypeRepository repository;

}
