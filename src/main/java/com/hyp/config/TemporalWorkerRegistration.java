package com.hyp.config;

import com.hyp.temporal.activities.OrderFulfillmentActivitiesImpl;
import com.hyp.temporal.activities.OrderPaymentActivitiesImpl;
import com.hyp.temporal.activities.OrderTrackActivitiesImpl;
import com.hyp.temporal.activities.RestaurantActivitiesImpl;
import com.hyp.temporal.activities.StockUpdateActivitiesImpl;
import com.hyp.temporal.service.OrderTrackWorkflowService;
import com.hyp.temporal.service.OrderWorkflowService;
import com.hyp.temporal.service.RestaurantWorkflowService;
import com.hyp.temporal.service.StockWorkflowService;
import com.hyp.temporal.workflow.OrderFulfillmentWorkflowImpl;
import com.hyp.temporal.workflow.OrderPaymentWorkflowImpl;
import com.hyp.temporal.workflow.OrderTrackWorkFlowImpl;
import com.hyp.temporal.workflow.RestaurantWorkflowImpl;
import com.hyp.temporal.workflow.StockUpdateWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TemporalWorkerRegistration {

    private final WorkerFactory factory;

    private final OrderPaymentActivitiesImpl paymentActivities;
    private final OrderFulfillmentActivitiesImpl orderFulfillmentActivities;
    private final RestaurantActivitiesImpl restaurantActivities;
    private final StockUpdateActivitiesImpl stockUpdateActivities;
    private final OrderTrackActivitiesImpl orderTrackActivitiesImpl;

    @PostConstruct
    public void registerWorkers() {
        Worker orderWorker = factory.newWorker(OrderWorkflowService.ORDER_TASK_QUEUE);
        orderWorker.registerWorkflowImplementationTypes(
                OrderPaymentWorkflowImpl.class, OrderFulfillmentWorkflowImpl.class);
        orderWorker.registerActivitiesImplementations(paymentActivities, orderFulfillmentActivities);

        Worker restaurantWorker = factory.newWorker(RestaurantWorkflowService.RESTAURANT_TASK_QUEUE);
        restaurantWorker.registerWorkflowImplementationTypes(RestaurantWorkflowImpl.class);
        restaurantWorker.registerActivitiesImplementations(restaurantActivities);

        Worker stockWorker = factory.newWorker(StockWorkflowService.STOCK_TASK_QUEUE);
        stockWorker.registerWorkflowImplementationTypes(StockUpdateWorkflowImpl.class);
        stockWorker.registerActivitiesImplementations(stockUpdateActivities);

        Worker orderTrackWorker = factory.newWorker(OrderTrackWorkflowService.ORDER_TRACK_QUEUE);
        orderTrackWorker.registerWorkflowImplementationTypes(OrderTrackWorkFlowImpl.class);
        orderTrackWorker.registerActivitiesImplementations(orderTrackActivitiesImpl);

        startWorkersWithRetry();
    }

    private void startWorkersWithRetry() {
        Thread starter = new Thread(
                () -> {
                    int attempt = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            factory.start();
                            log.info("Temporal workers started successfully");
                            return;
                        } catch (Exception e) {
                            attempt++;
                            long waitSeconds = Math.min(60, 5L * attempt); // cap at 60s
                            log.warn(
                                    "Temporal unavailable (attempt {}), retrying in {}s: {}",
                                    attempt,
                                    waitSeconds,
                                    e.getMessage());
                            try {
                                Thread.sleep(waitSeconds * 1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                },
                "temporal-worker-starter");

        starter.setDaemon(true);
        starter.start();
    }
}
