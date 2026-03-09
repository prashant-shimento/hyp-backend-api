package com.hyp.controller;

import com.hyp.dto.AffiliateDto;
import com.hyp.entity.Affiliate;
import com.hyp.service.AffiliateService;
import com.hyp.translation.AffiliateTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/affiliate")
public class AffiliateController extends BaseController<AffiliateDto, Affiliate, String> {
    @Autowired
    public AffiliateTranslation AffiliateTranslation;

    @Autowired
    public AffiliateService AffiliateService;
}
