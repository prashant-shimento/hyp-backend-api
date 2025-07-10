package com.hyp.temporal.workflow;

import com.hyp.temporal.activities.RestaurantActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class RestaurantWorkflowImpl implements RestaurantWorkflow {

    private final RestaurantActivities activities = Workflow.newActivityStub(
            RestaurantActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build()
    );

    @Override
    public void handleRestaurantStatus(String restaurantId, long delay) {
        log.info("Starting update status workflow for restaurant {} after {} delay",
                restaurantId, delay);
        Workflow.sleep(Duration.ofSeconds(delay));
        try {
            activities.updateStatus(restaurantId);
            log.info("Successfully turned on restaurant {} ", restaurantId);
        } catch (Exception e) {
            log.error("Failed to turned on restaurant {} ", restaurantId, e);
        }
    }
}
