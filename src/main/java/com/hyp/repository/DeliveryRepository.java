package com.hyp.repository;

import com.hyp.entity.Delivery;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryRepository extends MongoRepository<Delivery, String> {

    public Delivery findByOrderIdAndIsDeletedFalse(String orderId);

    public Delivery findByOrderId(String orderId);

    public Delivery findByDeliveryOrderIdAndIsDeletedFalse(String deliveryOrderId);

    public List<Delivery> findByOrderIdInAndIsDeletedFalse(List<String> orderIds);
}
