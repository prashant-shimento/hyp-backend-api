package com.hyp.event.payment;

import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import com.hyp.enums.OrderStatusType;
import com.hyp.event.EventHandler;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCreateEventHandler implements EventHandler<PaymentCreateEvent> {

    private final PaymentService paymentService;
    private final OrderService orderService;

    @Override
    public Class<PaymentCreateEvent> payloadType() {
        return PaymentCreateEvent.class;
    }

    @Override
    public void handle(PaymentCreateEvent event) throws Exception {
        String orderId = event.getOrderId();
        log.info("Processing PaymentCreateEvent for orderId={}", orderId);

        // Validate order exists
        Order order = orderService.findById(orderId);
        if (order == null) {
            log.warn("Order not found for orderId={}, skipping payment creation", orderId);
            return;
        }

        // Idempotency check - payment already exists?
        Payment existingPayment = paymentService.findByOrderId(orderId);
        if (existingPayment != null) {
            log.info("Payment already exists for orderId={}, skipping", orderId);
            return;
        }

        // Check order is in valid status (not cancelled)
        if (order.getStatus() == OrderStatusType.CANCELLED) {
            log.warn("Order {} is cancelled, skipping payment creation", orderId);
            return;
        }

        // Create payment via Razorpay
        Payment payment = paymentService.createPaymentOrder(order);
        log.info("Payment created by consumer for orderId={}, paymentOrderId={}", orderId, payment.getPaymentOrderId());
    }
}
