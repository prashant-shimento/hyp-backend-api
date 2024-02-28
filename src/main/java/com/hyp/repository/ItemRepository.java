package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hyp.entity.Item;

@Repository
public interface ItemRepository extends MongoRepository<Item, String> {

}