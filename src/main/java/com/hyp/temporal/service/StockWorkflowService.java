package com.hyp.temporal.service;

import com.hyp.request.PosStockRequest;
import com.hyp.temporal.workflow.StockUpdateWorkflow;
import com.hyp.util.CommonUtils;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockWorkflowService {

    private final WorkflowClient workflowClient;

    public static final String STOCK_TASK_QUEUE = "stock-task-queue";

    public void startStockUpdateWorkflow(PosStockRequest stockRequest, long delay) {
        String workflowId = CommonUtils.generateWorkflowId(stockRequest.getRestaurantId());
        try {
            StockUpdateWorkflow workflow = workflowClient.newWorkflowStub(
                    StockUpdateWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId("stock-update-" + workflowId)
                            .setTaskQueue(STOCK_TASK_QUEUE)
                            .build());
            WorkflowClient.start(workflow::handleStockUpdate, stockRequest, delay, workflowId);
            log.info(
                    "Started STOCK_UPDATE workflow restaurantId={} delay={} items={}",
                    stockRequest.getRestaurantId(),
                    delay,
                    stockRequest.getItemId().size());
        } catch (Exception e) {
            log.error("Failed to start STOCK_UPDATE workflow restaurantId={}", stockRequest.getRestaurantId(), e);
            throw e;
        }
    }
}
