package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Delivery;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {

	public Delivery findByOrderIdAndIsDeletedFalse(String orderId);
	
	public Delivery findByDeliveryOrderIdAndIsDeletedFalse(String deliveryOrderId);
}
