package com.hyp.temporal.workflow;

import com.hyp.enums.OrderStatusType;
import com.hyp.temporal.activities.OrderPaymentActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderPaymentWorkflowImpl implements OrderPaymentWorkflow {

    private OrderStatusType currentStatus = OrderStatusType.PAYMENT_PENDING;

    private final OrderPaymentActivities activities = Workflow.newActivityStub(
            OrderPaymentActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build());

    @Override
    public void handleOrderPayment(String orderId) {
        log.info("Payment workflow started for order {}", orderId);

        // Check order status first
        currentStatus =
                OrderStatusType.valueOf(activities.fetchOrderStatus(orderId).toUpperCase());
        if (currentStatus != OrderStatusType.PAYMENT_PENDING) {
            log.info("Order {} already resolved with status {}", orderId, currentStatus);
            return;
        }

        // Order is pending - check actual payment status from gateway
        String paymentStatus = activities.fetchPaymentStatus(orderId);
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            log.info("Payment already done for order {}, processing", orderId);
            activities.processPayment(orderId, paymentStatus);
            return;
        }

        // Wait for signal or timeout
        boolean resolved =
                Workflow.await(Duration.ofMinutes(45), () -> currentStatus != OrderStatusType.PAYMENT_PENDING);

        if (!resolved) {
            // Before dropping off, check payment gateway one more time
            paymentStatus = activities.fetchPaymentStatus(orderId);
            if ("paid".equalsIgnoreCase(paymentStatus)) {
                log.info("Payment found on timeout check for order {}, processing", orderId);
                activities.processPayment(orderId, paymentStatus);
                return;
            }
            log.warn("Payment timeout for order {}", orderId);
            activities.dropOffOrder(orderId);
            return;
        }

        log.info("Payment workflow completed for order {} with status {}", orderId, currentStatus);
    }

    @Override
    public void onPaymentStatusChanged(String newStatus) {
        try {
            OrderStatusType incoming = OrderStatusType.valueOf(newStatus.toUpperCase());

            if (currentStatus == OrderStatusType.PAYMENT_PENDING) {
                currentStatus = incoming;
                log.info("Workflow received status update: {}", incoming);
            } else {
                log.debug("Ignoring status update {} because payment already resolved as {}", incoming, currentStatus);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid order status received: {}", newStatus);
        }
    }
}
