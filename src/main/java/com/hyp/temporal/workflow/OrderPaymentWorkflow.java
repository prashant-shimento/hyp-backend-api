package com.hyp.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderPaymentWorkflow {
    @WorkflowMethod
    void handleOrderPayment(String orderId);
}
