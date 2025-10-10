package com.hyp.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderTrackWorkFlow {
    @WorkflowMethod
    public void handleOrderTrack(String orderId);
}
