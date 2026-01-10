package com.hyp.temporal.workflow;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderPaymentWorkflow {

    @WorkflowMethod
    void handleOrderPayment(String orderId);

    @SignalMethod
    void onPaymentStatusChanged(String newStatus);
}
