package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hyp.entity.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

}
