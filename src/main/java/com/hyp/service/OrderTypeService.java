package com.hyp.service;

import com.hyp.entity.OrderType;
import com.hyp.repository.OrderTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderTypeService extends BaseServiceImpl<OrderType, String> {
    @Autowired
    OrderTypeRepository orderTypeRepository;
}
