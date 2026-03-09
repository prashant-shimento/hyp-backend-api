package com.hyp.service;

import com.hyp.entity.Affiliate;
import com.hyp.repository.AffiliateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AffiliateService extends BaseServiceImpl<Affiliate, String> {
    @Autowired
    AffiliateRepository affiliateRepository;
}
