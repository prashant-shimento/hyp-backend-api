package com.hyp.repository;

import com.hyp.entity.SaasDashboard;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SaasDashboardRepository extends MongoRepository<SaasDashboard, String> {}
