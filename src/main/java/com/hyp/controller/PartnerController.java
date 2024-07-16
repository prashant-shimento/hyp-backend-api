package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.PartnerDto;
import com.hyp.entity.Partner;
import com.hyp.translation.PartnerTranslation;

@RestController
@RequestMapping("/partner")
public class PartnerController extends BaseController<PartnerDto, Partner, String> {

	@Autowired
	public PartnerTranslation partnerTranslation;
}
