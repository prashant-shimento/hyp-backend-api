package com.hyp.repository;

import com.hyp.entity.ReferralCode;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReferralCodeRepository extends MongoRepository<ReferralCode, String> {

    Optional<ReferralCode> findByCodeAndActiveTrueAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);
}
