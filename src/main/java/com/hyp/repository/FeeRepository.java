package com.hyp.repository;

import com.hyp.entity.Fee;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeeRepository extends MongoRepository<Fee, String> {

    Fee findByRestaurantId(String restaurantId);
}
