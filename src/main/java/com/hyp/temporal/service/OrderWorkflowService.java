package com.hyp.temporal.service;

import com.hyp.temporal.workflow.OrderFulfillmentWorkflow;
import com.hyp.temporal.workflow.OrderPaymentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
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
        try {
            OrderPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                    OrderPaymentWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId("order-payment-" + orderId)
                            .setTaskQueue(ORDER_TASK_QUEUE)
                            .build());
            WorkflowClient.start(workflow::handleOrderPayment, orderId);
            log.info("Started ORDER_PAYMENT workflow orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to start ORDER_PAYMENT workflow orderId={}", orderId, e);
            throw e;
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
