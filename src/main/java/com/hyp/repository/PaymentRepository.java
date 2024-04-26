package com.hyp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.entity.Payment;

public interface PaymentRepository extends MongoRepository<Payment, String> {
	
	Payment findByPaymentOrderId(String paymentOrderId);
	
	Payment findByOrderId(String orderId);
	
	Payment findByPaymentId(String paymentId);
}