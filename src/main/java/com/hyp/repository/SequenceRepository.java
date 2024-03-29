package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Sequence;

public interface SequenceRepository extends MongoRepository<Sequence, String>{

}
