package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.ApiLog;

public interface ApiLogRepository extends MongoRepository<ApiLog, String> {

}
