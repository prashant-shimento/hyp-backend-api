package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.entity.Attribute;
import com.hyp.repository.AttributeRepository;

import lombok.Getter;
import lombok.Setter;

@Component
public class AttributeServiceImpl extends BaseServiceImpl<Attribute, String,AttributeRepository> implements AttributeService {

	@Autowired
	private AttributeRepository repository;

}
