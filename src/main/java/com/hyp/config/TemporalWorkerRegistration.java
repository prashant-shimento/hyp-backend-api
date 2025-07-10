package com.hyp.config;


import com.hyp.temporal.activities.OrderFulfillmentActivitiesImpl;
import com.hyp.temporal.activities.OrderPaymentActivitiesImpl;
import com.hyp.temporal.activities.RestaurantActivitiesImpl;
import com.hyp.temporal.activities.StockUpdateActivitiesImpl;
import com.hyp.temporal.service.OrderWorkflowService;
import com.hyp.temporal.service.RestaurantWorkflowService;
import com.hyp.temporal.service.StockWorkflowService;
import com.hyp.temporal.workflow.OrderFulfillmentWorkflowImpl;
import com.hyp.temporal.workflow.OrderPaymentWorkflowImpl;
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

    // Inject all activities
    private final OrderPaymentActivitiesImpl paymentActivities;
    private final OrderFulfillmentActivitiesImpl orderFulfillmentActivities;
    private final RestaurantActivitiesImpl restaurantActivities;
    private final StockUpdateActivitiesImpl stockUpdateActivities;

    @PostConstruct
    public void registerWorkers() {
        log.info("Registering Temporal workers...");

        // Register Order workers
        Worker orderWorker = factory.newWorker(OrderWorkflowService.ORDER_TASK_QUEUE);
        orderWorker.registerWorkflowImplementationTypes(OrderPaymentWorkflowImpl.class, OrderFulfillmentWorkflowImpl.class);
        orderWorker.registerActivitiesImplementations(paymentActivities, orderFulfillmentActivities);

        // Register Restaurant workers
        Worker restaurantWorker = factory.newWorker(RestaurantWorkflowService.RESTAURANT_TASK_QUEUE);
        restaurantWorker.registerWorkflowImplementationTypes(RestaurantWorkflowImpl.class);
        restaurantWorker.registerActivitiesImplementations(restaurantActivities);

        // Register Stock workers
        Worker stockWorker = factory.newWorker(StockWorkflowService.STOCK_TASK_QUEUE);
        stockWorker.registerWorkflowImplementationTypes(StockUpdateWorkflowImpl.class);
        stockWorker.registerActivitiesImplementations(stockUpdateActivities);

        // Start the factory after all workers are registered
        factory.start();
        log.info("All Temporal workers registered and factory started.");
    }
}
