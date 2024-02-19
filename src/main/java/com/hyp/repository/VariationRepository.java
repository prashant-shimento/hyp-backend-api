package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Variation;

public interface VariationRepository extends MongoRepository<Variation, String> {

}