package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Attribute;
import com.hyp.repository.AttributeRepository;

@Service
public class AttributeService extends BaseServiceImpl<Attribute, String> {
	@Autowired
	AttributeRepository attributeRepository;
}
