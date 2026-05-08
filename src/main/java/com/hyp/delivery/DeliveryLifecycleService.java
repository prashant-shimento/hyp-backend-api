package com.hyp.delivery;

import com.hyp.entity.DeliveryEvent;
import com.hyp.enums.DeliveryLifecycleEvent;
import com.hyp.enums.DeliveryPartner;
import com.hyp.repository.DeliveryEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryLifecycleService {

    private final DeliveryEventRepository repository;

    /**
     * Append a delivery lifecycle event. Fire-and-forget — never blocks the caller.
     */
    public void record(
            String orderId,
            String deliveryId,
            String restaurantId,
            DeliveryPartner partner,
            DeliveryLifecycleEvent eventType,
            boolean success,
            Long durationMs,
            Map<String, Object> metadata) {
        CompletableFuture.runAsync(() -> {
            try {
                repository.save(DeliveryEvent.builder()
                        .orderId(orderId)
                        .deliveryId(deliveryId)
                        .restaurantId(restaurantId)
                        .partner(partner)
                        .eventType(eventType)
                        .success(success)
                        .durationMs(durationMs)
                        .metadata(metadata)
                        .timestamp(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                log.error("Failed to record delivery event orderId={} type={}", orderId, eventType, e);
            }
        });
    }

    public void record(
            String orderId,
            String deliveryId,
            String restaurantId,
            DeliveryPartner partner,
            DeliveryLifecycleEvent eventType,
            boolean success) {
        record(orderId, deliveryId, restaurantId, partner, eventType, success, null, null);
    }

    public List<DeliveryEvent> getByOrder(String orderId) {
        return repository.findByOrderIdOrderByTimestampAsc(orderId);
    }

    public List<DeliveryEvent> getByDelivery(String deliveryId) {
        return repository.findByDeliveryIdOrderByTimestampAsc(deliveryId);
    }

    public List<DeliveryEvent> getPartnerHistory(String restaurantId, DeliveryPartner partner) {
        return repository.findTop100ByRestaurantIdAndPartnerOrderByTimestampDesc(restaurantId, partner);
    }
}
