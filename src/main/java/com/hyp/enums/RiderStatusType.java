package com.hyp.enums;

public enum RiderStatusType {
	rider_assigned,
    rider_arrived,
    pickedup,
    delivered;
	
	public static RiderStatusType getRiderStatusByDeliveryRider(DeliveryFulfillStatusType status) {
		switch (status) {
		case OUT_FOR_PICKUP:
			return RiderStatusType.rider_assigned;
		case REACHED_PICKUP:
			return RiderStatusType.rider_arrived;
		case PICKED_UP:
			return RiderStatusType.pickedup;
		case DELIVERED:
			return RiderStatusType.delivered;
		default:
			return RiderStatusType.rider_assigned;
		}
	}
}
