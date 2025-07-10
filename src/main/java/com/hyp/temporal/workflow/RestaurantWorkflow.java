package com.hyp.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface RestaurantWorkflow {
    @WorkflowMethod
    void handleRestaurantStatus(String restaurantId, long delay);
}
