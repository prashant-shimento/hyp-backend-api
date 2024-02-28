package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.AddonItem;

public interface AddonItemRepository extends MongoRepository<AddonItem, String> {

}