package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.RiderRecord;

public interface RiderRecordRepository extends MongoRepository<RiderRecord, String> {

	RiderRecord findByRiderContact(String riderContact);

}
