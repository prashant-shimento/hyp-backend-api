package com.hyp.repository;

import com.hyp.entity.Report;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReportRepository extends MongoRepository<Report, String> {

    Report findByName(String name);
}
