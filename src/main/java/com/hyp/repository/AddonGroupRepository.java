package com.hyp.repository;

import com.hyp.entity.AddonGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AddonGroupRepository extends MongoRepository<AddonGroup, String> {}
