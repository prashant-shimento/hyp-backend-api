package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.entity.Tax;
import com.hyp.repository.TaxRepository;

@Service
public class TaxService extends BaseServiceImpl<Tax, String,TaxRepository>{

}
