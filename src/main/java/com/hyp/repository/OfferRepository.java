package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Offer;

public interface OfferRepository extends MongoRepository<Offer, String> {
}