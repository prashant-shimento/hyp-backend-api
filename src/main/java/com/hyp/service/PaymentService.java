package com.hyp.service;

import org.springframework.stereotype.Service;

import com.razorpay.Order;

@Service
public interface PaymentService {

	public Order createPaymentOrder(double amount);

}
