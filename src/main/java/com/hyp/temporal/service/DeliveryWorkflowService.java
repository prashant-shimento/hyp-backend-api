package com.hyp.temporal.service;

import com.hyp.temporal.workflow.DeliveryAssignmentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryWorkflowService {

    private final WorkflowClient workflowClient;

    public static final String DELIVERY_TASK_QUEUE = OrderWorkflowService.ORDER_TASK_QUEUE;

    private static final String WORKFLOW_ID_PREFIX = "delivery-assignment-";

    /**
     * Start an assignment-watch workflow for a newly created delivery.
     * The workflow waits for a rider-assigned signal or times out and switches provider.
     */
    public void startAssignmentWatch(String orderId, String deliveryId, String provider) {
        String workflowId = WORKFLOW_ID_PREFIX + deliveryId;
        try {
            DeliveryAssignmentWorkflow workflow = workflowClient.newWorkflowStub(
                    DeliveryAssignmentWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setWorkflowId(workflowId)
                            .setTaskQueue(DELIVERY_TASK_QUEUE)
                            .setWorkflowExecutionTimeout(Duration.ofMinutes(30))
                            .build());
            WorkflowClient.start(workflow::waitForAssignment, orderId, deliveryId, provider);
            log.info(
                    "Started delivery-assignment workflow orderId={} deliveryId={} provider={}",
                    orderId,
                    deliveryId,
                    provider);
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.warn("Delivery-assignment workflow already running for deliveryId={}", deliveryId);
        } catch (Exception e) {
            log.error("Failed to start delivery-assignment workflow deliveryId={}", deliveryId, e);
        }
    }

    /**
     * Signal the assignment workflow that a rider has been assigned.
     * Safe to call even if the workflow has already completed.
     */
    public void signalRiderAssigned(String deliveryId) {
        try {
            DeliveryAssignmentWorkflow workflow =
                    workflowClient.newWorkflowStub(DeliveryAssignmentWorkflow.class, WORKFLOW_ID_PREFIX + deliveryId);
            workflow.notifyRiderAssigned();
            log.info("Signalled riderAssigned for deliveryId={}", deliveryId);
        } catch (Exception e) {
            log.debug("Could not signal assignment workflow deliveryId={}: {}", deliveryId, e.getMessage());
        }
    }
}
