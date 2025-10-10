package com.hyp.controller;

import com.hyp.dto.OfferDto;
import com.hyp.entity.Offer;
import com.hyp.translation.OfferTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/offer")
public class OfferController extends BaseController<OfferDto, Offer, String> {

    @Autowired
    public OfferTranslation offerTranslation;
}
