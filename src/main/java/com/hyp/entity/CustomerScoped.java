package com.hyp.entity;

/**
 * Interface for entities owned by a customer (e.g., Address, Order, Payment).
 * Enables centralized customer-scoped access control in BaseServiceImpl.
 */
public interface CustomerScoped {
    String getCustomerId();

    void setCustomerId(String customerId);
}
