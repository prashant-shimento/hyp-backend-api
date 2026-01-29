package com.hyp.repository;

import com.hyp.entity.Offer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OfferRepository extends MongoRepository<Offer, String> {
    Offer findByOfferCode(String offerCode);
}
