package com.hyp.temporal.service;

import org.springframework.stereotype.Service;

import com.hyp.temporal.workflow.OrderTrackWorkFlow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTrackWorkflowService {
	private final WorkflowClient workflowClient;
	public static final String ORDER_TRACK_QUEUE = "order-track-queue";

	public void startOrderTrackWorkflow(String orderId) {
		OrderTrackWorkFlow workflow = workflowClient.newWorkflowStub(OrderTrackWorkFlow.class, WorkflowOptions
				.newBuilder().setWorkflowId("order-track-" + orderId).setTaskQueue(ORDER_TRACK_QUEUE).build());
		WorkflowClient.start(workflow::handleOrderTrack, orderId);
	}

}
