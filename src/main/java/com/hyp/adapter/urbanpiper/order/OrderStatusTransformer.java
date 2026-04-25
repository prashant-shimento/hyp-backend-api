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
            case "placed", "created" -> OrderStatusType.PLACED;
            case "acknowledged" -> OrderStatusType.ACKNOWLEDGED;
            case "accepted" -> OrderStatusType.ACCEPTED;
            case "rejected" -> OrderStatusType.REJECTED;
            case "food_ready", "ready" -> OrderStatusType.FOOD_READY;
            case "dispatched" -> OrderStatusType.DISPATCHED;
            case "out_for_delivery" -> OrderStatusType.OUT_FOR_DELIVERY;
            case "completed", "delivered" -> OrderStatusType.DELIVERED;
            case "cancelled" -> OrderStatusType.CANCELLED;
            case "failed" -> OrderStatusType.FAILED;
            default -> {
                log.warn("Unknown UrbanPiper status: {}, defaulting to PROCESSING", urbanPiperStatus);
                yield OrderStatusType.PROCESSING;
            }
        };
    }
}
