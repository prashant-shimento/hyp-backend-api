package com.hyp.temporal.service;

import com.hyp.temporal.workflow.OrderTrackWorkFlow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTrackWorkflowService {
    private final WorkflowClient workflowClient;
    public static final String ORDER_TRACK_QUEUE = "order-track-queue";

    public void startOrderTrackWorkflow(String orderId) {
        String workflowId = "order-track-" + orderId;
        OrderTrackWorkFlow workflow = workflowClient.newWorkflowStub(
                OrderTrackWorkFlow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(ORDER_TRACK_QUEUE)
                        .build());
        try {
            WorkflowClient.start(workflow::handleOrderTrack, orderId);
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.warn("Workflow already running for {}", workflowId);
        }
    }
}
