package com.hyp.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;
import com.hyp.repository.PartnerRepository;

@Service
public class PartnerService extends BaseServiceImpl<Partner, String> {
	
	@Autowired
	PartnerRepository partnerRepository;

	public List<Partner> findByPartnerType(PartnerType type) {
        return partnerRepository.findByType(type);
    }
	
	public Partner findPartnersByRestaurantId(String restaurantId, PartnerType type) {
        return partnerRepository.findByRestaurantsContainingAndType(restaurantId, type);
    }
}
