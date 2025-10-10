package com.hyp.repository;

import com.hyp.entity.ApiLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ApiLogRepository extends MongoRepository<ApiLog, String> {}
