package com.hyp.repository;

import com.hyp.entity.OfferUsage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferUsageRepository extends MongoRepository<OfferUsage, String> {
    // Integer countByOfferCodeAndCustomerId(String offerId, String customerId);

    OfferUsage findByCustomerIdAndOfferCode(String customerId, String offerCode);
}
