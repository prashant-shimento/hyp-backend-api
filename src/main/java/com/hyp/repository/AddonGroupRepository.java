package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.AddonGroup;

public interface AddonGroupRepository extends MongoRepository<AddonGroup, String> {

}