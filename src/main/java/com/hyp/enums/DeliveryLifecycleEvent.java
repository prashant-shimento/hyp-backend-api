package com.hyp.enums;

public enum DeliveryLifecycleEvent {

    // Provider API events
    ORDER_CREATED, // createOrder() API call succeeded
    API_FAILURE, // createOrder() API call failed

    // Rider journey events (from callbacks/webhooks)
    RIDER_ASSIGNED, // rider accepted the order
    RIDER_REACHED_PICKUP, // rider arrived at restaurant
    RIDER_PICKED_UP, // rider picked up the order
    RIDER_DELIVERED, // order delivered to customer

    // SLA / system events
    ASSIGNMENT_TIMEOUT, // rider not assigned within SLA window
    PICKUP_TIMEOUT, // rider not at pickup within SLA window
    DELIVERY_TIMEOUT, // order not delivered within SLA window
    CANCELLED, // order cancelled (any party: restaurant, customer, system)
    PROVIDER_SWITCHED // switched from one provider to another (fallback triggered)
}
