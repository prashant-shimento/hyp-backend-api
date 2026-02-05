package com.hyp.service;

import com.hyp.entity.Customer;
import com.hyp.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomerService extends BaseServiceImpl<Customer, String> {

    private static final String CACHE_NAME = "customers";

    @Autowired
    public CustomerRepository customerRepository;

    @Autowired
    CacheService cacheService;

    /**
     * Get customer by ID using two-level cache.
     * L1 (Caffeine) → L2 (Redis) → MongoDB
     */
    @Override
    public Customer findById(String id) {
        if (id == null) return null;

        return cacheService.getOrLoad(CACHE_NAME, id, Customer.class, () -> {
            log.debug("Loading customer from DB: {}", id);
            return super.findById(id);
        });
    }

    /**
     * Get customer by mobile using two-level cache.
     */
    public Customer findByMobile(String mobile) {
        if (mobile == null) return null;

        return cacheService.getOrLoad(CACHE_NAME, "mobile:" + mobile, Customer.class, () -> {
            log.debug("Loading customer by mobile from DB: {}", mobile);
            return customerRepository.findByMobile(mobile);
        });
    }

    /**
     * Save customer and evict from BOTH L1 and L2 caches.
     */
    @Override
    public Customer save(Customer entity) {
        Customer saved = super.save(entity);

        // Evict from both caches
        cacheService.evict(CACHE_NAME, entity.getId());
        if (entity.getMobile() != null) {
            cacheService.evict(CACHE_NAME, "mobile:" + entity.getMobile());
        }

        log.debug("Customer saved and cache evicted: {}", entity.getId());
        return saved;
    }

    /**
     * Update customer and evict from BOTH L1 and L2 caches.
     */
    @Override
    public Customer update(Customer entity) {
        Customer updated = super.update(entity);

        // Evict from both caches
        cacheService.evict(CACHE_NAME, entity.getId());
        if (entity.getMobile() != null) {
            cacheService.evict(CACHE_NAME, "mobile:" + entity.getMobile());
        }

        log.debug("Customer updated and cache evicted: {}", entity.getId());
        return updated;
    }
}
