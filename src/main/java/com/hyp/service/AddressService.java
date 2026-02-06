package com.hyp.service;

import com.hyp.entity.Address;
import com.hyp.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AddressService extends BaseServiceImpl<Address, String> {

    @Autowired
    public AddressRepository addressRepository;

    @Override
    protected String cacheName() {
        return "addresses";
    }

    @Override
    protected Class<Address> entityType() {
        return Address.class;
    }
}
