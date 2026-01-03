package com.hyp.temporal.activities;

import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import com.hyp.enums.OrderStatusType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.PaymentException;
import com.hyp.observability.ObservabilityContext;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        ObservabilityContext.setOrderId(orderId);
        try {
            return paymentService.fetchPaymentOrderStatus(orderId);
        } catch (PaymentException e) {
            throw new RuntimeException("Failed to fetch payment status", e);
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public void verifyPayment(String orderId, String paymentStatus) {
        ObservabilityContext.setOrderId(orderId);
        try {
            log.info("Verifying payment via workflow");
            Order order = Optional.ofNullable(orderService.findById(orderId))
                    .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
            Payment payment = Optional.ofNullable(paymentService.findByOrderId(orderId))
                    .orElseThrow(() -> new EntityNotFoundException("Payment", orderId));
            paymentService.verifyPayment(order, payment, paymentStatus);
        } catch (EntityNotFoundException | PaymentException e) {
            throw new RuntimeException("Failed to verify payment", e);
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public String fetchOrderStatus(String orderId) {
        Order order = orderService.findById(orderId);
        return order.getStatus().name();
    }

    @Override
    public void dropOffOrder(String orderId) {
        ObservabilityContext.setOrderId(orderId);
        try {
            Order order = orderService.findById(orderId);
            order.setStatus(OrderStatusType.DROPPED_OFF);
            orderService.save(order);
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public void initiateRefund(String orderId, boolean instantRefund) {
        ObservabilityContext.setOrderId(orderId);
        log.info("Initiating refund instantRefund={}", instantRefund);
        try {
            Order order = orderService.findById(orderId);
            paymentService.createRefund(order.getId(), order.getGrandTotalAmount(), instantRefund, "Order Cancelled");
            order.setStatus(OrderStatusType.REFUND_INITIATED);
            orderService.save(order);
            log.info("Refund initiated amount={}", order.getGrandTotalAmount());
        } catch (Exception e) {
            log.error("Refund failed", e);
            throw new RuntimeException("Refund initiation failed", e);
        } finally {
            ObservabilityContext.clear();
        }
    }
}
