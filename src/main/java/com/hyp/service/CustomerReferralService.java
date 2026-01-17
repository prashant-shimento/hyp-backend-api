package com.hyp.service;

import com.hyp.entity.CustomerReferral;
import com.hyp.repository.CustomerReferralRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerReferralService extends BaseServiceImpl<CustomerReferral, String> {

    @Autowired
    public CustomerReferralRepository customerReferralRepository;
}
