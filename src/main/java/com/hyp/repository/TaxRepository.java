package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Tax;

public interface TaxRepository extends MongoRepository<Tax, String> {

}