package com.hyp.temporal.workflow;

import com.hyp.constants.Constants;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.enums.DeliveryOrderStatusType;
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
        log.info("Waiting for {} minutes before fulfilling order {}", fulfillmentDelay, orderId);
        Workflow.sleep(Duration.ofMinutes(fulfillmentDelay));
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
            log.warn("Delivery not found for order ID: {}", orderId);
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

        log.info("Waiting 2 minutes to verify delivery status for order {}", orderId);
        Workflow.sleep(Duration.ofMinutes(2));

        Delivery updatedDelivery = activities.fetchDelivery(orderId);
        if (updatedDelivery != null && updatedDelivery.getStatus() == DeliveryOrderStatusType.FULFILLED) {
            log.info("Order {} fulfillment confirmed.", orderId);
            return;
        }

        log.warn("Order {} delivery still pending after 2 minutes. Sending alert.", orderId);
        activities.sendAlert(orderId);
    }
}
