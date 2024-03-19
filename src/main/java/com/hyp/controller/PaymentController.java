package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.service.PaymentService;
import com.razorpay.Order;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
	
	@Autowired
	PaymentService paymentService;
	
	@PostMapping()
	public void createPaymentOrder(double amount) {
		Order order = paymentService.createPaymentOrder(amount);
	}

}
