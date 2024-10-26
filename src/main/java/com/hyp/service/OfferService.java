package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Offer;
import com.hyp.repository.OfferRepository;

@Service
public class OfferService extends BaseServiceImpl<Offer, String> {

	@Autowired
	OfferRepository offerRepository;
}
