package com.hyp.controller;

import com.hyp.dto.OfferUsageDto;
import com.hyp.entity.OfferUsage;
import com.hyp.translation.OfferUsageTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/offer-usage")
public class OfferUsageController extends BaseController<OfferUsageDto, OfferUsage, String> {

    @Autowired
    public OfferUsageTranslation offerTranslation;
}
