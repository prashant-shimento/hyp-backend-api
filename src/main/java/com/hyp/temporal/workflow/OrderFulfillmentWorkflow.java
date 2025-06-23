package com.hyp.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderFulfillmentWorkflow {
    @WorkflowMethod
    void handleOrderFulfillment(String orderId, int fulfillmentDelay);
}
