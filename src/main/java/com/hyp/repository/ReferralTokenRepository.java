package com.hyp.repository;

import com.hyp.entity.ReferralToken;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReferralTokenRepository extends MongoRepository<ReferralToken, String> {

    Optional<ReferralToken> findByToken(String token);
}
