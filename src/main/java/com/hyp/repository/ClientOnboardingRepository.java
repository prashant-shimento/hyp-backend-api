package com.hyp.repository;

import com.hyp.entity.ClientOnboarding;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientOnboardingRepository extends MongoRepository<ClientOnboarding, String> {}
