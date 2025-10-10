package com.hyp.repository;

import com.hyp.entity.AddonItem;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AddonItemRepository extends MongoRepository<AddonItem, String> {}
