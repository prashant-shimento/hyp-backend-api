package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.AttributeDto;
import com.hyp.entity.Attribute;
import com.hyp.translation.AttributeTranslation;

@RestController
@RequestMapping("/api/attribute")
public class AttributeController extends BaseController<AttributeDto, Attribute, String> {

	@Autowired
	AttributeTranslation attributeTranslation;
}
