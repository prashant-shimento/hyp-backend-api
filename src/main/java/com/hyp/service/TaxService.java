package com.hyp.service;

import com.hyp.entity.Tax;
import com.hyp.repository.TaxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaxService extends BaseServiceImpl<Tax, String> {
    @Autowired
    TaxRepository taxRepository;
}
