package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Report;

public interface ReportRepository extends MongoRepository<Report, String> {
	
	Report findByName(String name);
}
