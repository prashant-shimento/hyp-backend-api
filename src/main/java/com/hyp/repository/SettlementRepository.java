package com.hyp.repository;

import com.hyp.entity.Settlement;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SettlementRepository extends MongoRepository<Settlement, String> {
    Settlement findByOrderId(String orderId);
}
