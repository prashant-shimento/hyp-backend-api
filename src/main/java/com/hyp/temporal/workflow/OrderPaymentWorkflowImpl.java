package com.hyp.temporal.workflow;

import com.hyp.enums.OrderStatusType;
import com.hyp.temporal.activities.OrderPaymentActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderPaymentWorkflowImpl implements OrderPaymentWorkflow {

    private static final int MAX_MINUTES = 45;

    private OrderStatusType currentStatus = OrderStatusType.PAYMENT_PENDING;

    private final OrderPaymentActivities activities = Workflow.newActivityStub(
            OrderPaymentActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build());

    @Override
    public void handleOrderPayment(String orderId) {
        log.info("Payment workflow started for order {}", orderId);

        for (int minute = 1; minute <= MAX_MINUTES; minute++) {

            boolean signaled =
                    Workflow.await(Duration.ofMinutes(1), () -> currentStatus != OrderStatusType.PAYMENT_PENDING);

            if (signaled) {
                log.info(
                        "Workflow stopped via signal at minute {} for order {}, status={}",
                        minute,
                        orderId,
                        currentStatus);
                return;
            }

            String paymentStatus = activities.fetchPaymentStatus(orderId);

            if ("paid".equalsIgnoreCase(paymentStatus)) {
                log.info("Payment detected at minute {} for order {}, processing", minute, orderId);
                activities.processPayment(orderId, paymentStatus);
                return;
            }

            log.debug("Minute {}: payment still pending for order {}", minute, orderId);
        }

        log.warn("Payment still pending after {} minutes, dropping order {}", MAX_MINUTES, orderId);
        activities.dropOffOrder(orderId);
    }

    @Override
    public void onPaymentStatusChanged(String newStatus) {
        try {
            OrderStatusType incoming = OrderStatusType.valueOf(newStatus.toUpperCase());

            if (currentStatus == OrderStatusType.PAYMENT_PENDING
                    && (incoming == OrderStatusType.PAID || incoming == OrderStatusType.CANCELLED)) {

                currentStatus = incoming;
                log.info("Workflow resolved via signal: {}", incoming);
            } else {
                log.debug("Ignoring signal {} in state {}", incoming, currentStatus);
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid order status received: {}", newStatus);
        }
    }
}
