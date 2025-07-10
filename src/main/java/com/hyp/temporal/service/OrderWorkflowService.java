package com.hyp.temporal.service;

import com.hyp.temporal.activities.OrderFulfillmentActivitiesImpl;
import com.hyp.temporal.activities.OrderPaymentActivitiesImpl;
import com.hyp.temporal.workflow.OrderFulfillmentWorkflow;
import com.hyp.temporal.workflow.OrderFulfillmentWorkflowImpl;
import com.hyp.temporal.workflow.OrderPaymentWorkflow;
import com.hyp.temporal.workflow.OrderPaymentWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private final WorkflowClient workflowClient;
    public static final String ORDER_TASK_QUEUE = "order-task-queue";

    public void startOrderPaymentWorkflow(String orderId) {
        OrderPaymentWorkflow workflow = workflowClient.newWorkflowStub(
                OrderPaymentWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("order-payment-" + orderId)
                        .setTaskQueue(ORDER_TASK_QUEUE)
                        .build()
        );
        WorkflowClient.start(workflow::handleOrderPayment, orderId);
    }

    public void startOrderFulfillmentWorkflow(String orderId, int fulfillmentDelay) {
        OrderFulfillmentWorkflow workflow = workflowClient.newWorkflowStub(
                OrderFulfillmentWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("order-fulfillment-" + orderId)
                        .setTaskQueue(ORDER_TASK_QUEUE)
                        .build()
        );
        WorkflowClient.start(workflow::handleOrderFulfillment, orderId, fulfillmentDelay);
    }

}
