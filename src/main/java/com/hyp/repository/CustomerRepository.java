package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Customer;

public interface CustomerRepository extends MongoRepository<Customer, String> {

	Customer findByMobile(String mobile);
}
