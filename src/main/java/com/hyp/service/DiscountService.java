package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.entity.Discount;
import com.hyp.repository.DiscountRepository;

@Service
public class DiscountService extends BaseServiceImpl<Discount, String,DiscountRepository> {

}
