package com.hyp.delivery.adloggs;

import com.hyp.enums.DeliveryFulfillStatusType;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class AdloggsStatusMapper {

    public static DeliveryFulfillStatusType map(int statusId) {
        return switch (statusId) {
            case 2 -> DeliveryFulfillStatusType.CREATED; // Pending
            case 3 -> DeliveryFulfillStatusType.OUT_FOR_PICKUP; // Assigned
            case 11 -> DeliveryFulfillStatusType.REACHED_PICKUP; // Arrived at pickup
            case 4 -> DeliveryFulfillStatusType.PICKED_UP; // Picked Up
            case 9 -> DeliveryFulfillStatusType.OUT_FOR_DELIVERY; // Out for Delivery
            case 8 -> DeliveryFulfillStatusType.REACHED_DELIVERY; // Arrived at delivery
            case 5 -> DeliveryFulfillStatusType.DELIVERED;
            case 6 -> DeliveryFulfillStatusType.CANCELLED;
            case 13 -> DeliveryFulfillStatusType.RTO_OUT_FOR_DELIVERY; // Return initiated
            case 14 -> DeliveryFulfillStatusType.RTO_DELIVERED;
            default -> throw new IllegalArgumentException("Unknown Adloggs status id: " + statusId);
        };
    }
}
