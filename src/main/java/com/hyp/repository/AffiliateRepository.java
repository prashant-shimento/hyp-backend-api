package com.hyp.repository;

import com.hyp.entity.Affiliate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AffiliateRepository extends MongoRepository<Affiliate, String> {}
