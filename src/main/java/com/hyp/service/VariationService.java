package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Variation;
import com.hyp.repository.VariationRepository;

@Service
public class VariationService extends BaseServiceImpl<Variation, String> {

	@Autowired
	VariationRepository variationRepository;
}
