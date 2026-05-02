package com.hyp.entity;

/**
 * Interface for entities where the entity's own ID is the principal's user ID.
 * Used for self-access scoping (e.g., Customer, User can only access their own record).
 * Requires the entity to also implement Identifiable.
 */
public interface SelfScoped {}
