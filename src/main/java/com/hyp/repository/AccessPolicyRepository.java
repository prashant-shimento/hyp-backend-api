package com.hyp.repository;

import com.hyp.entity.AccessPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessPolicyRepository extends MongoRepository<AccessPolicy, String> {

    List<AccessPolicy> findByActiveTrue();

    List<AccessPolicy> findByActiveTrueOrderByPriorityDesc();

    List<AccessPolicy> findByResourceAndOperation(String resource, String operation);

    List<AccessPolicy> findByResourceAndActiveTrue(String resource);

    Optional<AccessPolicy> findByResourceAndOperationAndActiveTrue(String resource, String operation);

    boolean existsByResourceAndOperation(String resource, String operation);

    List<AccessPolicy> findByResourceContainingAndOperation(String resource, String operation);

    List<AccessPolicy> findByResourceContaining(String resource);
}
