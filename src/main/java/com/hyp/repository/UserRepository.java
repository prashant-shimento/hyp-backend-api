package com.hyp.repository;

import com.hyp.entity.User;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

    User findByEmail(String email);

    /**
     * Find all users that have access to a specific restaurant.
     */
    List<User> findByRestaurantIdsContaining(String restaurantId);

    /**
     * Check if any user exists with access to a specific restaurant.
     */
    boolean existsByRestaurantIdsContaining(String restaurantId);
}
