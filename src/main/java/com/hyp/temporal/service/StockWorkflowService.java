package com.hyp.temporal.service;

import com.hyp.request.PosStockRequest;
import com.hyp.temporal.workflow.StockUpdateWorkflow;
import com.hyp.util.CommonUtils;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockWorkflowService {

    private final WorkflowClient workflowClient;
    public static final String STOCK_TASK_QUEUE = "stock-task-queue";

    public void startStockUpdateWorkflow(PosStockRequest stockRequest, long delay) {
        StockUpdateWorkflow workflow = workflowClient.newWorkflowStub(
                StockUpdateWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("stock-update-" + CommonUtils.generateWorkflowId(stockRequest.getRestaurantId()))
                        .setTaskQueue(STOCK_TASK_QUEUE)
                        .build()
        );
        WorkflowClient.start(workflow::handleStockUpdate,stockRequest, delay);
    }

}
