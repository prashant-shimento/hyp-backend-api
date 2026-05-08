package com.hyp.delivery;

import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryPartner;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryEstimate;
import com.hyp.model.DeliveryOrderStatus;

public interface DeliveryProvider {

    DeliveryPartner getType();

    /**
     * Fetch price estimate. Returns available=false (never throws) if unreachable
     * or location is unserviceable.
     */
    DeliveryEstimate getEstimate(Restaurant restaurant, Address address);

    record CreateOrderResult(String orderId, Double fee) {}

    /**
     * Create a delivery order. Returns the provider order ID and actual fee from the create response.
     * Throws DeliveryException on failure — triggers fallback in DeliveryRouter.
     */
    CreateOrderResult createOrder(Restaurant restaurant, Address address, Customer customer, Order order)
            throws DeliveryException;

    /**
     * Cancel or unallocate based on partner's state machine.
     * Throws DeliveryException if not possible (e.g. post-pickup).
     */
    void cancelOrder(Delivery delivery) throws DeliveryException;

    /** Map partner-specific raw status string to our unified DeliveryFulfillStatusType. */
    DeliveryFulfillStatusType mapStatus(String rawStatus);

    /** True while it is still possible to switch to another partner. */
    boolean isSwitchable(DeliveryFulfillStatusType currentStatus);

    /** Get current order status from the partner's API. */
    DeliveryOrderStatus getOrderStatus(Delivery delivery) throws DeliveryException;
}
