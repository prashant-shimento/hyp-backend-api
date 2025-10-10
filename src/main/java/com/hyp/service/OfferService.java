package com.hyp.service;

import com.hyp.entity.Offer;
import com.hyp.repository.OfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OfferService extends BaseServiceImpl<Offer, String> {

    @Autowired
    OfferRepository offerRepository;
}
