package com.hyp.repository;

import com.hyp.entity.IntegrationKey;
import com.hyp.entity.IntegrationKey.WebhookType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntegrationKeyRepository extends MongoRepository<IntegrationKey, String> {

    Optional<IntegrationKey> findByApiKey(String apiKey);

    Optional<IntegrationKey> findByApiKeyAndActiveTrue(String apiKey);

    List<IntegrationKey> findByPartnerId(String partnerId);

    List<IntegrationKey> findByWebhookType(WebhookType webhookType);

    List<IntegrationKey> findByActiveTrue();

    boolean existsByApiKey(String apiKey);
}
