package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.hyp.entity.Item;


public interface ItemRepository extends MongoRepository<Item, String> {

}