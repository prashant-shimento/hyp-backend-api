package com.hyp.temporal.workflow;

import com.hyp.constants.Constants;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.DeliveryPartner;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.DeliveryException;
import com.hyp.temporal.activities.OrderFulfillmentActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderFulfillmentWorkflowImpl implements OrderFulfillmentWorkflow {

    private final OrderFulfillmentActivities activities = Workflow.newActivityStub(
            OrderFulfillmentActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build());

    @Override
    public void handleOrderFulfillment(String orderId, int fulfillmentDelay) {
        try {
            int delayMinutes = Math.max(0, fulfillmentDelay);
            log.info("Waiting for {} minutes before fulfilling order {}", delayMinutes, orderId);
            if (delayMinutes > 0) {
                Workflow.sleep(Duration.ofMinutes(delayMinutes));
            }

            Order order = activities.fetchOrder(orderId);
            if (order == null) {
                log.warn("Order not found for ID: {}", orderId);
                return;
            }

            if (!(order.getStatus() == OrderStatusType.ACCEPTED
                    || order.getStatus() == OrderStatusType.READY_FOR_DELIVERY)) {
                log.info("Skipping fulfillment. Order {} is in status {}", orderId, order.getStatus());
                return;
            }

            Delivery delivery = activities.fetchDelivery(orderId);
            if (delivery == null) {
                // Pre-orders only: delivery is created here (1h before scheduled time).
                // Regular orders have delivery created at POS ACCEPTED, before this workflow fires.
                log.info("No delivery for pre-order {} — creating now", orderId);
                delivery = activities.createDelivery(order);
            }

            if (delivery == null) {
                log.warn("Delivery creation failed for orderId={}", orderId);
                return;
            }

            // Adloggs auto-assigns rider on creation — no explicit fulfill step.
            if (delivery.getProvider() == DeliveryPartner.ADLOGGS) {
                log.info("Adloggs order — rider auto-assigned, no fulfill step for orderId={}", orderId);
                return;
            }

            String fulfillType = activities.getFulfillmentMode();
            try {
                activities.fulfillDelivery(delivery, Constants.SYSTEM, fulfillType);
                log.info("Order {} successfully fulfilled using '{}' mode.", orderId, fulfillType);
            } catch (DeliveryException e) {
                log.error("Error fulfilling order {}: {}", orderId, e.getMessage(), e);
                activities.updateOrderStatus(orderId, OrderStatusType.DELIVERY_ERROR);
            }

            Workflow.sleep(Duration.ofMinutes(2));

            Delivery updatedDelivery = activities.fetchDelivery(orderId);
            if (updatedDelivery != null && updatedDelivery.getStatus() == DeliveryOrderStatusType.FULFILLED) {
                log.info("Order {} fulfillment confirmed.", orderId);
                return;
            }

            log.warn("Order {} delivery still pending after 2 minutes. Sending alert.", orderId);
            activities.sendAlert(orderId);

        } catch (Exception e) {
            log.error("Workflow failed for order {}", orderId, e);
            throw Workflow.wrap(e);
        }
    }
}
