package com.hyp.repository;

import com.hyp.entity.CustomerTestimonial;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerTestimonialRepository extends MongoRepository<CustomerTestimonial, String> {}
