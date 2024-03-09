package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Order;

public interface OrderRepository extends MongoRepository<Order, String> {

}