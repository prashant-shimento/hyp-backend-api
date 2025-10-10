package com.hyp.repository;

import com.hyp.entity.Variation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VariationRepository extends MongoRepository<Variation, String> {}
