package com.hyp.enums;

public enum RiderStatusType {
    rider_assigned("rider-assigned"),
    rider_arrived("rider-arrived"),
    pickedup("pickedup"),
    delivered("delivered");

    private final String value;

    RiderStatusType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static RiderStatusType getRiderStatusByDeliveryRider(DeliveryFulfillStatusType status) {
        return switch (status) {
            case REACHED_PICKUP -> RiderStatusType.rider_arrived;
            case PICKED_UP -> RiderStatusType.pickedup;
            case DELIVERED -> RiderStatusType.delivered;
            default -> RiderStatusType.rider_assigned;
        };
    }

    public static RiderStatusType getRiderStatusByOrderStatusType(OrderStatusType status) {
        return switch (status) {
            case REACHED_PICKUP -> RiderStatusType.rider_arrived;
            case PICKED_UP -> RiderStatusType.pickedup;
            case DELIVERED -> RiderStatusType.delivered;
            default -> RiderStatusType.rider_assigned;
        };
    }
}
