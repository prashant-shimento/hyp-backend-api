package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Address;

public interface AddressRepository extends MongoRepository<Address, String> {

}
