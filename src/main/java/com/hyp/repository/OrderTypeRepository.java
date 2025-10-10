package com.hyp.repository;

import com.hyp.entity.OrderType;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderTypeRepository extends MongoRepository<OrderType, String> {}
