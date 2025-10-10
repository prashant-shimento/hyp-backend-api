package com.hyp.temporal.workflow;

import com.hyp.request.PosStockRequest;
import com.hyp.temporal.activities.StockUpdateActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StockUpdateWorkflowImpl implements StockUpdateWorkflow {

    private final StockUpdateActivities activities = Workflow.newActivityStub(
            StockUpdateActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build());

    @Override
    public void handleStockUpdate(PosStockRequest stockRequest, long delay, String workflowId) {
        Workflow.sleep(Duration.ofSeconds(delay));
        String type = stockRequest.getType();
        String restaurantId = stockRequest.getRestaurantId();
        log.info(
                "Starting auto turn-on workflow for restaurant {} and {} items after {} delay in workflow {}",
                restaurantId,
                stockRequest.getItemId().size(),
                delay,
                workflowId);
        try {
            activities.updateStock(stockRequest.getItemId(), type, true);
            log.info(
                    "Successfully turned on {} items of type {} for restaurant {} in workflow {}",
                    stockRequest.getItemId().size(),
                    type,
                    restaurantId,
                    workflowId);
        } catch (Exception e) {
            log.error(
                    "Failed to auto turn-on items: {} for restaurant {} in workflow {}",
                    stockRequest.getItemId().size(),
                    restaurantId,
                    workflowId,
                    e);
        }
    }
}
