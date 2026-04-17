package com.hyp.repository;

import com.hyp.entity.Settlement;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SettlementRepository extends MongoRepository<Settlement, String> {
    Settlement findByOrderId(String orderId);

    List<Settlement> findByOrderIdIn(List<String> orderIds);
}
