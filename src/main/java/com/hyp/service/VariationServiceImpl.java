package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.entity.Variation;
import com.hyp.repository.VariationRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class VariationServiceImpl extends BaseServiceImpl<Variation, String,VariationRepository> implements VariationService {

	@Autowired
	private VariationRepository repository;

}
