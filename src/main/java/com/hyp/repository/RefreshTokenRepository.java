package com.hyp.repository;

import com.hyp.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserIdAndRevokedFalse(String userId);

    List<RefreshToken> findByUserId(String userId);

    void deleteByUserId(String userId);

    void deleteByToken(String token);

    long countByUserIdAndRevokedFalse(String userId);
}
