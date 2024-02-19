package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Discount;

public interface DiscountRepository extends MongoRepository<Discount, String> {

}