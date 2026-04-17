package com.hyp.service;

import com.hyp.entity.Offer;
import com.hyp.repository.OfferRepository;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OfferService extends BaseServiceImpl<Offer, String> {

    private static final String CACHE_NAME = "offers";

    @Autowired
    OfferRepository offerRepository;

    @Override
    protected String cacheName() {
        return CACHE_NAME;
    }

    @Override
    protected Class<Offer> entityType() {
        return Offer.class;
    }

    @Override
    protected List<String> additionalEvictionKeys(Offer entity) {
        if (entity.getOfferCode() != null) {
            return List.of("code:" + entity.getOfferCode());
        }
        return Collections.emptyList();
    }

    public Offer findByOfferCode(String offerCode) {
        if (offerCode == null) return null;
        return cacheService()
                .getOrLoad(
                        CACHE_NAME, "code:" + offerCode, Offer.class, () -> offerRepository.findByOfferCode(offerCode));
    }
}
