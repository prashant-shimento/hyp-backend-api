package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.entity.Tax;
import com.hyp.repository.TaxRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class TaxServiceImpl extends BaseServiceImpl<Tax, String,TaxRepository> implements TaxService {

	@Autowired
	private TaxRepository repository;

}
