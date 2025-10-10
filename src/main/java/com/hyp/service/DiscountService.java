package com.hyp.service;

import com.hyp.entity.Discount;
import com.hyp.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiscountService extends BaseServiceImpl<Discount, String> {
    @Autowired
    DiscountRepository discountRepository;
}
