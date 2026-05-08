package com.hyp.repository;

import com.hyp.entity.DeliveryEvent;
import com.hyp.enums.DeliveryLifecycleEvent;
import com.hyp.enums.DeliveryPartner;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeliveryEventRepository extends MongoRepository<DeliveryEvent, String> {

    List<DeliveryEvent> findByOrderIdOrderByTimestampAsc(String orderId);

    List<DeliveryEvent> findByDeliveryIdOrderByTimestampAsc(String deliveryId);

    List<DeliveryEvent> findTop100ByRestaurantIdAndPartnerOrderByTimestampDesc(
            String restaurantId, DeliveryPartner partner);

    List<DeliveryEvent> findTop100ByRestaurantIdAndPartnerAndEventTypeOrderByTimestampDesc(
            String restaurantId, DeliveryPartner partner, DeliveryLifecycleEvent eventType);
}
