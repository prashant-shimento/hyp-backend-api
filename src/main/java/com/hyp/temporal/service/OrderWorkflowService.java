package com.hyp.temporal.service;

import com.hyp.temporal.workflow.OrderFulfillmentWorkflow;
import com.hyp.temporal.workflow.OrderPaymentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private final WorkflowClient workflowClient;

    public static final String ORDER_TASK_QUEUE = "order-task-queue";

    public void startOrderPaymentWorkflow(String orderId) {
        String workflowId = "order-payment-" + orderId;
        try {
            OrderPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                    OrderPaymentWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(ORDER_TASK_QUEUE)
                            .setWorkflowExecutionTimeout(Duration.ofMinutes(50))
                            .build());

            WorkflowClient.start(workflow::handleOrderPayment, orderId);
            log.info("Started ORDER_PAYMENT workflow orderId={}", orderId);

        } catch (WorkflowExecutionAlreadyStarted e) {
            log.warn("ORDER_PAYMENT workflow already running for orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to start ORDER_PAYMENT workflow orderId={}", orderId, e);
            throw e;
        }
    }

    public void signalPaymentStatusChanged(String orderId, String newStatus) {
        try {
            OrderPaymentWorkflow workflow =
                    workflowClient.newWorkflowStub(OrderPaymentWorkflow.class, "order-payment-" + orderId);
            workflow.onPaymentStatusChanged(newStatus);
            log.info("Signalled payment status change orderId={} status={}", orderId, newStatus);
        } catch (Exception e) {
            log.debug("Failed to signal payment workflow orderId={}: {}", orderId, e.getMessage());
        }
    }

    public void startOrderFulfillmentWorkflow(String orderId, int fulfillmentDelay) {
        try {
            OrderFulfillmentWorkflow workflow = workflowClient.newWorkflowStub(
                    OrderFulfillmentWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId("order-fulfillment-" + orderId)
                            .setTaskQueue(ORDER_TASK_QUEUE)
                            .build());
            WorkflowClient.start(workflow::handleOrderFulfillment, orderId, fulfillmentDelay);
            log.info("Started ORDER_FULFILLMENT workflow orderId={} delay={}", orderId, fulfillmentDelay);
        } catch (Exception e) {
            log.error("Failed to start ORDER_FULFILLMENT workflow orderId={}", orderId, e);
            throw e;
        }
    }

    public void startPreOrderWorkflow(String orderId, int scheduledDelay) {
        try {
            OrderFulfillmentWorkflow workflow = workflowClient.newWorkflowStub(
                    OrderFulfillmentWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId("pre-order-fulfillment-" + orderId)
                            .setTaskQueue(ORDER_TASK_QUEUE)
                            .build());
            WorkflowClient.start(workflow::handleOrderFulfillment, orderId, scheduledDelay);
            log.info("Started PRE_ORDER_FULFILLMENT workflow orderId={} delay={}", orderId, scheduledDelay);
        } catch (Exception e) {
            log.error("Failed to start PRE_ORDER_FULFILLMENT workflow orderId={}", orderId, e);
            throw e;
        }
    }
}
