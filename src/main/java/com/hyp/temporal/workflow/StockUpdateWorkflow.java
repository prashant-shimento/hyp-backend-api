package com.hyp.temporal.workflow;

import com.hyp.request.PosStockRequest;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface StockUpdateWorkflow {
    @WorkflowMethod
    void handleStockUpdate(PosStockRequest stockRequest, long delay, String workflowId);
}
