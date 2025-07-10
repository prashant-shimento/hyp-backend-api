package com.hyp.temporal.workflow;

import com.hyp.enums.OrderStatusType;
import com.hyp.temporal.activities.OrderPaymentActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class OrderPaymentWorkflowImpl implements OrderPaymentWorkflow{

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
                    .build()
    );

    @Override
    public void handleOrderPayment(String orderId) {
        log.info("Workflow started: handleOrderPayment for orderId={}", orderId);
        final int maxAttempts = 15;

        for (int minute = 1; minute <= maxAttempts; minute++) {
            Workflow.sleep(Duration.ofMinutes(1));

            String currentOrderStatus = activities.fetchOrderStatus(orderId);
            OrderStatusType currentStatus = OrderStatusType.valueOf(currentOrderStatus.toUpperCase());
            if (currentStatus.ordinal() >= OrderStatusType.PAID.ordinal()) {
                log.info("Order {} marked as PAID. Exiting workflow early {}th attempt", orderId, minute);
                return;
            }
            String paymentStatus = activities.fetchPaymentStatus(orderId);
            log.info("Fetched payment status for orderId={} => {}", orderId, paymentStatus);
            activities.verifyPayment(orderId, paymentStatus);
        }

        String finalStatus = activities.fetchOrderStatus(orderId);
        if (OrderStatusType.PAYMENT_PENDING.name().equalsIgnoreCase(finalStatus)) {
            log.warn("Payment still pending - Marking order as dropped off for orderId={}", orderId);
            activities.dropOffOrder(orderId);
        }
        log.info("Workflow completed: handleOrderPayment for orderId={}", orderId);
    }
}
