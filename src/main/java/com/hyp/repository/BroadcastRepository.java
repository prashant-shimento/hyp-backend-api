package com.hyp.repository;

import com.hyp.entity.Broadcast;
import com.hyp.enums.BroadcastScope;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BroadcastRepository extends MongoRepository<Broadcast, String> {

    List<Broadcast> findByScopeAndReceiverIdAndActiveTrue(BroadcastScope scope, String receiverId);
}
