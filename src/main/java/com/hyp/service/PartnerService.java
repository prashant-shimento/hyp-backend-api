package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Partner;
import com.hyp.repository.PartnerRepository;

@Service
public class PartnerService extends BaseServiceImpl<Partner, String> {
	
	@Autowired
	PartnerRepository partnerRepository;
}
