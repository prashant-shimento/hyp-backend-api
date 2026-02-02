package com.hyp.service;

import com.hyp.entity.OfferUsage;
import com.hyp.repository.OfferUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OfferUsageService extends BaseServiceImpl<OfferUsage, String> {

    @Autowired
    OfferUsageRepository offerUsageRepository;

    public Integer getUsageCount(String offerCode, String customerId) {
        OfferUsage usage = offerUsageRepository.findByCustomerIdAndOfferCode(customerId, offerCode);
        return usage != null && usage.getUsageCount() != null ? usage.getUsageCount() : 0;
    }

    public OfferUsage getCustomerIdAndOfferCode(String customerId, String offerCode) {
        return offerUsageRepository.findByCustomerIdAndOfferCode(customerId, offerCode);
    }
}
