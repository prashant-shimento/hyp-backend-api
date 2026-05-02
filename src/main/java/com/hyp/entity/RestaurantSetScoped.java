package com.hyp.entity;

/**
 * Marker interface for entities that belong to multiple restaurants via a Set<String> restaurants field.
 * Unlike RestaurantScoped (single restaurantId), these entities are filtered with
 * { restaurants: restaurantId } — MongoDB matches the value against array elements.
 *
 * Example: Customer belongs to many restaurants (one per order placed).
 */
public interface RestaurantSetScoped {}
