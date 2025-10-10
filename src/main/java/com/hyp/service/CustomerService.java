package com.hyp.service;

import com.hyp.entity.Customer;
import com.hyp.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerService extends BaseServiceImpl<Customer, String> {

    @Autowired
    public CustomerRepository customerRepository;

    public Customer findByMobile(String mobile) {
        return customerRepository.findByMobile(mobile);
    }
}
