package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Attribute;

public interface AttributeRepository extends MongoRepository<Attribute, String> {

}