package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.OrderType;

public interface OrderTypeRepository extends MongoRepository<OrderType, String> {

}