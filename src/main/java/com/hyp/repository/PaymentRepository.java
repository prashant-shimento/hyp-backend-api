package com.hyp.repository;

import com.hyp.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    Payment findByPaymentOrderId(String paymentOrderId);

    Payment findByOrderId(String orderId);

    Payment findByPaymentId(String paymentId);
}
