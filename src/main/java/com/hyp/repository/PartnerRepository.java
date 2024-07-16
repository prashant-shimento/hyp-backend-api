package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Partner;

public interface PartnerRepository extends MongoRepository<Partner, String> {

}
