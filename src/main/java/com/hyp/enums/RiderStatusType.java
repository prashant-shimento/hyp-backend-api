package com.hyp.enums;

public enum RiderStatusType {
	rider_assigned,
    rider_arrived,
    pickedup,
    delivered;
	
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
