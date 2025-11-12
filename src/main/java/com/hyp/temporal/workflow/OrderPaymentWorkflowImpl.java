package com.hyp.temporal.workflow;

import com.hyp.enums.OrderStatusType;
import com.hyp.temporal.activities.OrderPaymentActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderPaymentWorkflowImpl implements OrderPaymentWorkflow {

    private final OrderPaymentActivities activities = Workflow.newActivityStub(
            OrderPaymentActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(5))
                            .setBackoffCoefficient(2)
                            .setMaximumInterval(Duration.ofMinutes(1))
                            .setMaximumAttempts(5)
                            .build())
                    .build());

    @Override
    public void handleOrderPayment(String orderId) {
        log.info("Workflow started: handleOrderPayment for orderId={}", orderId);
        try {
            final int maxAttempts = 45;
            boolean paid = false;
            int paidAtMinute = -1;

            for (int minute = 1; minute <= maxAttempts; minute++) {
                Workflow.sleep(Duration.ofMinutes(1));

                String currentOrderStatus = activities.fetchOrderStatus(orderId);
                OrderStatusType currentStatus = OrderStatusType.valueOf(currentOrderStatus.toUpperCase());

                if (currentStatus == OrderStatusType.PAID) {
                    paid = true;
                    paidAtMinute = minute;
                    log.info("Order {} marked as PAID at {}th minute", orderId, minute);
                    break;
                }

                String paymentStatus = activities.fetchPaymentStatus(orderId);
                activities.verifyPayment(orderId, paymentStatus);
            }

            String finalStatus = activities.fetchOrderStatus(orderId);

            if (paid) {
                log.info("Order {} paid within {} minutes (at {}). No refund.", orderId, maxAttempts, paidAtMinute);
            } else if (OrderStatusType.PAID.name().equalsIgnoreCase(finalStatus)) {
                log.warn("Order {} paid AFTER {} minutes. Initiating refund.", orderId, maxAttempts);
                activities.initiateRefund(orderId, true);
            } else if (OrderStatusType.PAYMENT_PENDING.name().equalsIgnoreCase(finalStatus)) {
                log.warn("Order {} still pending after {} minutes. Dropping off.", orderId, maxAttempts);
                activities.dropOffOrder(orderId);
            } else {
                log.info("Order {} final status after {} minutes: {}.", orderId, maxAttempts, finalStatus);
            }

            log.info("Workflow completed: handleOrderPayment for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Workflow failed for order {}", orderId, e);
            throw Workflow.wrap(e);
        }
    }
}
