package com.hyp.repository;

import com.hyp.entity.DeliveryQuoteRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryQuoteRepository extends MongoRepository<DeliveryQuoteRecord, String> {}
