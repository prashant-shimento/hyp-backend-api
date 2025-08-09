package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.CustomerTestimonial;

public interface CustomerTestimonialRepository extends MongoRepository<CustomerTestimonial, String> {

}
