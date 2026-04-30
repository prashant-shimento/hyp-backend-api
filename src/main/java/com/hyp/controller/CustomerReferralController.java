package com.hyp.controller;

import com.hyp.dto.CustomerReferralDto;
import com.hyp.entity.CustomerReferral;
import com.hyp.service.CustomerReferralService;
import com.hyp.translation.CustomerReferralTranslation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = {"/api/v2/customer-referral", "/api/v3/customer-referral"})
public class CustomerReferralController extends BaseListController<CustomerReferralDto, CustomerReferral, String> {

    @Autowired
    private CustomerReferralTranslation customerReferralTranslation;

    @Autowired
    private CustomerReferralService customerReferralService;
}
