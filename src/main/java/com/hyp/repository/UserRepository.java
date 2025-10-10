package com.hyp.repository;

import com.hyp.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

    User findByEmail(String email);

    User findByRestaurantId(String restaurantId);
}
