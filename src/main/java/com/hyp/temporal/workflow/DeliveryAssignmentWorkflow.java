package com.hyp.temporal.workflow;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DeliveryAssignmentWorkflow {

    @WorkflowMethod
    void waitForAssignment(String orderId, String deliveryId, String provider);

    /** Signal sent when a rider-assigned callback is received for this delivery. */
    @SignalMethod
    void notifyRiderAssigned();
}
