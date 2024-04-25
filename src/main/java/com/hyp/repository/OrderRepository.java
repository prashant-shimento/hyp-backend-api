package com.hyp.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Order;
import com.hyp.enums.OrderStatusType;

public interface OrderRepository extends MongoRepository<Order, String> {

	public List<Order> findByStatus(OrderStatusType status);
}