package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Content;

public interface ContentRepository extends MongoRepository<Content, String> {

}
