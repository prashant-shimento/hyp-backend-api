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
import io.temporal.failure.ApplicationFailure;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentActivitiesImpl implements OrderPaymentActivities {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final OrderEventPublisher orderEventPublisher;

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
    public void processPayment(String orderId, String paymentStatus) {
        ObservabilityContext.setOrderId(orderId);
        try {
            Order order = Optional.ofNullable(orderService.findById(orderId))
                    .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
            Payment payment = Optional.ofNullable(paymentService.findByOrderId(orderId))
                    .orElseThrow(() -> new EntityNotFoundException("Payment", orderId));
            paymentService.processPayment(order, payment, paymentStatus);
        } catch (EntityNotFoundException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "ENTITY_NOT_FOUND", e);
        } catch (PaymentException e) {
            throw new RuntimeException("Failed to verify payment", e);
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public String fetchOrderStatus(String orderId) {
        ObservabilityContext.setOrderId(orderId);
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                throw ApplicationFailure.newNonRetryableFailure("Order not found: " + orderId, "ORDER_NOT_FOUND");
            }
            return order.getStatus().name();
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Override
    public void dropOffOrder(String orderId) {
        ObservabilityContext.setOrderId(orderId);
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                throw ApplicationFailure.newNonRetryableFailure("Order not found: " + orderId, "ORDER_NOT_FOUND");
            }
            order.setStatus(OrderStatusType.DROPPED_OFF);
            orderService.save(order);
            log.info("Order dropped off orderId={}", orderId);
        } finally {
            ObservabilityContext.clear();
        }
    }
}
