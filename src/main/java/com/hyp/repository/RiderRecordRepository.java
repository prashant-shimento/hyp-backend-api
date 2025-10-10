package com.hyp.repository;

import com.hyp.entity.RiderRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RiderRecordRepository extends MongoRepository<RiderRecord, String> {

    RiderRecord findByRiderContact(String riderContact);
}
