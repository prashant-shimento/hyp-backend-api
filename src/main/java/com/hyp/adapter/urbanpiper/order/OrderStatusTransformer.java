package com.hyp.adapter.urbanpiper.order;

import com.hyp.enums.OrderStatusType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderStatusTransformer {

    public OrderStatusType mapUrbanPiperStatus(String urbanPiperStatus) {
        if (urbanPiperStatus == null) {
            return OrderStatusType.PROCESSING;
        }
        
        return switch (urbanPiperStatus.toLowerCase()) {
            case "placed", "created" -> OrderStatusType.CREATED;
            case "acknowledged", "accepted" -> OrderStatusType.ACCEPTED;
            case "food_ready", "ready" -> OrderStatusType.READY_FOR_DELIVERY;
            case "dispatched" -> OrderStatusType.DISPATCHED;
            case "completed", "delivered" -> OrderStatusType.DELIVERED;
            case "cancelled", "rejected" -> OrderStatusType.CANCELLED;
            default -> {
                log.warn("Unknown UrbanPiper status: {}, defaulting to PROCESSING", urbanPiperStatus);
                yield OrderStatusType.PROCESSING;
            }
        };
    }
}
