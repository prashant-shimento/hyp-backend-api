package com.hyp.enums;

public enum OrderStatusType {
	CREATED, CONFIRMED, PAYMENT_PENDING, PAID, PAYMENT_FAILED, PROCESSING, ACCEPTED, DISPATCHED, READY_FOR_DELIVERY,
	RIDER_ASSIGNED, OUT_FOR_PICKUP, REACHED_PICKUP, PICKED_UP, OUT_FOR_DELIVERY, IN_TRANSIT, REACHED_DELIVERY,
	DELIVERED,DELIVERY_CANCELLATION, CANCELLED, ERROR, POS_ERROR, DELIVERY_ERROR, PAYMENT_ERROR, REFUND_INITIATED,REFUND_COMPLETED,REFUND_FAILED,REFUND_PENDING;

	public static OrderStatusType getOrderStatusByPosStatus(String value) {
		switch (value) {
		case "-1":
			return CANCELLED;
		case "4":
			return DISPATCHED;
		case "5":
			return READY_FOR_DELIVERY;
		case "10":
			return DELIVERED;
		case "1":
		case "2":
		case "3":
			return ACCEPTED;
		default:
			return PROCESSING;
		}
	}

	public static OrderStatusType getOrderStatusByDelvieryStatus(DeliveryFulfillStatusType value) {
		switch (value) {
		case CREATED:
			return RIDER_ASSIGNED;
		case OUT_FOR_PICKUP:
			return OUT_FOR_PICKUP;
		case REACHED_PICKUP:
			return REACHED_PICKUP;
		case PICKED_UP:
			return PICKED_UP;
		case OUT_FOR_DELIVERY:
			return OUT_FOR_DELIVERY;
		case REACHED_DELIVERY:
			return REACHED_DELIVERY;
		case DELIVERED:
			return DELIVERED;
		case CANCELLED:
			return DELIVERY_CANCELLATION;
		default:
			return RIDER_ASSIGNED;
		}
	}
	
	public static OrderStatusType getOrderStatusByRefundStatus(String value) {
		switch (value) {
		case "pending":
			return REFUND_INITIATED;
		case "processed":
			return REFUND_COMPLETED;
		case "failed":
			return REFUND_FAILED;
		default:
			return REFUND_PENDING;
		}
	}
}
