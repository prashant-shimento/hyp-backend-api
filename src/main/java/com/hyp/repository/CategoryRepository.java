package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Category;


public interface CategoryRepository extends MongoRepository<Category, String> {

}