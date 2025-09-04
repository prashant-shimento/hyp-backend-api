package com.hyp.temporal.activities;

import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import com.hyp.enums.OrderStatusType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.PaymentException;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class OrderPaymentActivitiesImpl implements OrderPaymentActivities {

    @Autowired
    PaymentService paymentService;
    @Autowired
    OrderService orderService;
    @Autowired
    OrderEventPublisher orderEventPublisher;

    @Override
    public String fetchPaymentStatus(String orderId) {
        try {
            return paymentService.fetchPaymentOrderStatus(orderId);
        } catch (PaymentException e) {
            throw new RuntimeException("Failed to fetch payment status", e);
        }
    }

    @Override
    public void verifyPayment(String orderId, String paymentStatus) {
        try {
            log.info("Payment Verification via Workflow");
            Order order = Optional.ofNullable(orderService.findById(orderId))
                    .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
            Payment payment = Optional.ofNullable(paymentService.findByOrderId(orderId))
                    .orElseThrow(() -> new EntityNotFoundException("Payment", orderId));
            paymentService.verifyPayment(order, payment, paymentStatus);
        } catch (EntityNotFoundException | PaymentException e) {
            throw new RuntimeException("Failed to verify payment", e);
        }

    }

    @Override
    public String fetchOrderStatus(String orderId) {
        Order order = orderService.findById(orderId);
        return order.getStatus().name();
    }

	@Override
	public void dropOffOrder(String orderId) {
		Order order = orderService.findById(orderId);
		order.setStatus(OrderStatusType.DROPPED_OFF);
		orderService.save(order);
	}

	@Override
	public void initiateRefund(String orderId, boolean instantRefund) {
		log.info("initiateRefund orderId={} instantRefund={}", orderId, instantRefund);
		try {
			Order order = orderService.findById(orderId);

			paymentService.createRefund(order.getId(), order.getGrandTotalAmount(), instantRefund, "Order Cancelled");

			order.setStatus(OrderStatusType.REFUND_INITIATED);
			orderService.save(order);

			log.info("refund initiated for orderId={} amount={}", orderId, order.getGrandTotalAmount());
		} catch (Exception e) {
			log.error("refund failed for orderId={}", orderId, e);
			throw new RuntimeException("Refund initiation failed", e);
		}
	}

}
