package com.hyp.repository;

import com.hyp.entity.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

    Restaurant findByMenuSharingCode(String menusharingcode);
}
