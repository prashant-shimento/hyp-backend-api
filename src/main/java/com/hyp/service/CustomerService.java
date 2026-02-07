package com.hyp.service;

import com.hyp.entity.Customer;
import com.hyp.repository.CustomerRepository;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomerService extends BaseServiceImpl<Customer, String> {

    private static final String CACHE_NAME = "customers";

    @Autowired
    public CustomerRepository customerRepository;

    @Override
    protected String cacheName() {
        return CACHE_NAME;
    }

    @Override
    protected Class<Customer> entityType() {
        return Customer.class;
    }

    @Override
    protected List<String> additionalEvictionKeys(Customer entity) {
        if (entity.getMobile() != null) {
            return List.of("mobile:" + entity.getMobile());
        }
        return Collections.emptyList();
    }

    /**
     * Get customer by mobile using two-level cache.
     */
    public Customer findByMobile(String mobile) {
        if (mobile == null) return null;

        return cacheService().getOrLoad(CACHE_NAME, "mobile:" + mobile, Customer.class, () -> {
            log.debug("Loading customer by mobile from DB: {}", mobile);
            return customerRepository.findByMobile(mobile);
        });
    }
}
