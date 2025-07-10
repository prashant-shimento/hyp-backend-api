package com.hyp.temporal.workflow;

import com.hyp.request.PosStockRequest;
import com.hyp.temporal.activities.StockUpdateActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class StockUpdateWorkflowImpl implements StockUpdateWorkflow{

    private final StockUpdateActivities activities = Workflow.newActivityStub(
            StockUpdateActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(2))
                    .build()
    );

    @Override
    public void handleStockUpdate(PosStockRequest stockRequest, long delay) {
        Workflow.sleep(Duration.ofSeconds(delay));
        String type = stockRequest.getType();
        String restaurantId = stockRequest.getRestaurantId();
        log.info("Starting auto turn-on workflow for restaurant {} and {} items after {} delay",
                restaurantId, stockRequest.getItemId().size(), delay);
        try {
            activities.updateStock(stockRequest.getItemId(), type, true);
            log.info("Successfully turned on {} items of type {} for restaurant {} ", stockRequest.getItemId().size(), type, restaurantId);
        } catch (Exception e) {
            log.error("Failed to auto turn-on items: {} for restaurant {} ", stockRequest.getItemId().size(), restaurantId, e);
        }
    }
}
