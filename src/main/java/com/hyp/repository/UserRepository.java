package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.User;

public interface UserRepository extends MongoRepository<User, String> {

	User findByEmail(String email);

	User findByRestaurantId(String restaurantId);

}
