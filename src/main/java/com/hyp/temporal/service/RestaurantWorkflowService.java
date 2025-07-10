package com.hyp.temporal.service;

import com.hyp.temporal.workflow.RestaurantWorkflow;
import com.hyp.util.CommonUtils;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantWorkflowService {

    private final WorkflowClient workflowClient;
    public static final String RESTAURANT_TASK_QUEUE = "restaurant-task-queue";

    public void startRestaurantStatusWorkflow(String restaurantId, long delay) {
        RestaurantWorkflow workflow = workflowClient.newWorkflowStub(
                RestaurantWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("restaurant-status-update-" + CommonUtils.generateWorkflowId(restaurantId))
                        .setTaskQueue(RESTAURANT_TASK_QUEUE)
                        .build()
        );
        WorkflowClient.start(workflow::handleRestaurantStatus, restaurantId, delay);
    }
}
