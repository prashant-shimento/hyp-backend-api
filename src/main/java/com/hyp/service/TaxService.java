package com.hyp.service;

import com.hyp.entity.Tax;
import com.hyp.repository.TaxRepository;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaxService extends BaseServiceImpl<Tax, String> {
    @Autowired
    TaxRepository taxRepository;

    @Override
    protected String cacheName() {
        return "taxes";
    }

    @Override
    protected Class<Tax> entityType() {
        return Tax.class;
    }

    public List<Tax> findAllByIdIn(List<String> requestTaxIds) {
        if (requestTaxIds == null || requestTaxIds.isEmpty()) {
            return Collections.emptyList();
        }
        return taxRepository.findAllByIdIn(requestTaxIds);
    }
}
