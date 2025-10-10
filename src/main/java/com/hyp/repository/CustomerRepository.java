package com.hyp.repository;

import com.hyp.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {

    Customer findByMobile(String mobile);
}
