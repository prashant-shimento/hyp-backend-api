package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.entity.Discount;
import com.hyp.repository.DiscountRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class DiscountServiceImpl extends BaseServiceImpl<Discount, String,DiscountRepository> implements DiscountService {

	@Autowired
	private DiscountRepository repository;

}
