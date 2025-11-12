package com.hyp.repository;

import com.hyp.entity.Order;
import com.hyp.enums.OrderStatusType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByRestaurantIdAndStatusInAndCreatedAtBetween(
            String restaurantId, OrderStatusType statusType, LocalDateTime startDate, LocalDateTime endDate);
}
