package com.hyp.temporal.workflow;

import com.hyp.temporal.activities.DeliveryAssignmentActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeliveryAssignmentWorkflowImpl implements DeliveryAssignmentWorkflow {

    private boolean riderAssigned = false;

    private final DeliveryAssignmentActivities activities = Workflow.newActivityStub(
            DeliveryAssignmentActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(3))
                    .build());

    @Override
    public void waitForAssignment(String orderId, String deliveryId, String provider) {
        int timeoutMinutes = activities.getAssignmentTimeoutMinutes(orderId);
        log.info(
                "Watching rider assignment orderId={} deliveryId={} provider={} timeout={}m",
                orderId,
                deliveryId,
                provider,
                timeoutMinutes);

        boolean assigned = Workflow.await(Duration.ofMinutes(timeoutMinutes), () -> riderAssigned);

        if (assigned) {
            log.info("Rider assigned in time orderId={}", orderId);
            return;
        }

        log.warn("Rider assignment timeout orderId={} deliveryId={} — switching provider", orderId, deliveryId);
        activities.switchDeliveryProvider(orderId, deliveryId);
    }

    @Override
    public void notifyRiderAssigned() {
        this.riderAssigned = true;
    }
}
