package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hyp.entity.Restaurant;

public interface RestaurantRepository extends MongoRepository<Restaurant, String> {

	Restaurant findByMenuSharingCode(String menusharingcode);

}