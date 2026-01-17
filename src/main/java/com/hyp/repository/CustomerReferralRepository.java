package com.hyp.repository;

import com.hyp.entity.CustomerReferral;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerReferralRepository extends MongoRepository<CustomerReferral, String> {}
