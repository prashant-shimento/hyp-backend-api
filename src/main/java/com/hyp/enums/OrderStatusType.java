package com.hyp.enums;

public enum OrderStatusType {
    CREATED,
    CONFIRMED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    PROCESSING,
    ACCEPTED,
    DISPATCHED,
    READY_FOR_DELIVERY,
    SEARCHING_RIDER,
    RIDER_ASSIGNED,
    OUT_FOR_PICKUP,
    REACHED_PICKUP,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    IN_TRANSIT,
    REACHED_DELIVERY,
    DELIVERED,
    CANCELLED,
    DELIVERY_CANCELLED,
    DROPPED_OFF,
    ERROR,
    POS_ERROR,
    DELIVERY_ERROR,
    PAYMENT_ERROR,
    REFUND_INITIATED,
    REFUND_COMPLETED,
    REFUND_FAILED,
    REFUND_PENDING,
    RIDER_CANCELLED,
    MANUAL_DELIVERY_BOOKED;

    public static OrderStatusType getOrderStatusByPosStatus(String value) {
        return switch (value) {
            case "-1" -> CANCELLED;
            case "4" -> DISPATCHED;
            case "5" -> READY_FOR_DELIVERY;
            case "10" -> DELIVERED;
            case "1", "2", "3" -> ACCEPTED;
            default -> PROCESSING;
        };
    }

    public static OrderStatusType getOrderStatusByPaymentStatus(String value) {
        return switch (value) {
            case "captured", "authorized" -> PAID;
            case "failed" -> PAYMENT_FAILED;
            default -> PROCESSING;
        };
    }

    public static OrderStatusType getOrderStatusByDeliveryStatus(DeliveryFulfillStatusType value) {
        return switch (value) {
            case OUT_FOR_PICKUP -> OUT_FOR_PICKUP;
            case REACHED_PICKUP -> REACHED_PICKUP;
            case PICKED_UP -> PICKED_UP;
            case OUT_FOR_DELIVERY -> OUT_FOR_DELIVERY;
            case REACHED_DELIVERY -> REACHED_DELIVERY;
            case DELIVERED -> DELIVERED;
            case CANCELLED -> RIDER_CANCELLED;
            default -> SEARCHING_RIDER;
        };
    }

    public static OrderStatusType getOrderStatusByRefundStatus(String value) {
        return switch (value) {
            case "pending" -> REFUND_INITIATED;
            case "processed" -> REFUND_COMPLETED;
            case "failed" -> REFUND_FAILED;
            default -> REFUND_PENDING;
        };
    }
}
