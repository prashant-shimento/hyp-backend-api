package com.hyp.repository;

import com.hyp.entity.Delivery;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {

    Delivery findFirstByOrderIdAndIsDeletedFalseOrderByCreatedAtDesc(String orderId);

    Delivery findFirstByOrderIdOrderByCreatedAtDesc(String orderId);

    Delivery findByDeliveryOrderIdAndIsDeletedFalse(String deliveryOrderId);

    Delivery findByDeliveryOrderId(String deliveryOrderId);

    List<Delivery> findByOrderIdInAndIsDeletedFalse(List<String> orderIds);
}
