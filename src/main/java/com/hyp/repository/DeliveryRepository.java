package com.hyp.repository;

import com.hyp.entity.Delivery;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {

    public Delivery findByOrderIdAndIsDeletedFalse(String orderId);

    public Delivery findByDeliveryOrderIdAndIsDeletedFalse(String deliveryOrderId);
}
