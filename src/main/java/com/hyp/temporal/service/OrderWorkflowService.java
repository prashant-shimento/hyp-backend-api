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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderWorkflowService {

    @Autowired
    WorkflowClient workflowClient;

    public static final String ORDER_TASK_QUEUE = "order-task-queue";

    @Autowired
    public OrderWorkflowService(WorkerFactory factory,
                                OrderPaymentActivitiesImpl paymentActivities,
                                OrderFulfillmentActivitiesImpl orderFulfillmentActivities) {

        Worker worker = factory.newWorker(ORDER_TASK_QUEUE);

        // Register Workflows
        worker.registerWorkflowImplementationTypes(OrderPaymentWorkflowImpl.class, OrderFulfillmentWorkflowImpl.class);
        // Register Activities
        worker.registerActivitiesImplementations(paymentActivities, orderFulfillmentActivities);


        factory.start();
    }

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
