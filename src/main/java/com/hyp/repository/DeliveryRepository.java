package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Delivery;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {

	public Delivery findByOrderId(String orderId);
}
