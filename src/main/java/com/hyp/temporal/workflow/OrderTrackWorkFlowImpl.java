package com.hyp.temporal.workflow;

import com.hyp.temporal.activities.OrderTrackActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderTrackWorkFlowImpl implements OrderTrackWorkFlow {

    private final OrderTrackActivities activities = Workflow.newActivityStub(
            OrderTrackActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .build());

    private String nextExpectedFor(String currentStatus) {
        if (currentStatus == null) return "progress to next stage";
        return switch (currentStatus.trim().toUpperCase()) {
            case "PAID" -> "ACCEPTED";
            case "ACCEPTED" -> "SEARCHING_RIDER";
            case "SEARCHING_RIDER" -> "OUT_FOR_PICKUP";
            case "OUT_FOR_PICKUP" -> "OUT_FOR_DELIVERY";
            case "OUT_FOR_DELIVERY", "DELIVERED" -> "DELIVERED";
            default -> "progress to the next expected status";
        };
    }

    @Override
    public void handleOrderTrack(String orderId) {
        log.info("Workflow started: handleOrderTrack for orderId={}", orderId);

        try {
            long effectiveStartTimeMs = Workflow.currentTimeMillis();

            final int MAX_WAIT_MINUTES = 90;

            final int[] alertTimings = {5, 7, 17, 30, 45, 50};
            final String[] expectedStatuses = {
                "ACCEPTED", "SEARCHING_RIDER", "OUT_FOR_PICKUP",
                "OUT_FOR_DELIVERY", "DELIVERED", "DELIVERED"
            };
            final String[] currentStatuses = {
                "PAID", "ACCEPTED", "SEARCHING_RIDER",
                "OUT_FOR_PICKUP", "OUT_FOR_DELIVERY", "OUT_FOR_DELIVERY"
            };

            boolean[] alertsSent = new boolean[alertTimings.length];
            int currentStartIndex = 0;

            String prevActualStatus = activities.fetchOrderStatus(orderId);

            while (true) {
                Workflow.sleep(Duration.ofMinutes(1));

                long nowMs = Workflow.currentTimeMillis();
                long minutesSinceCreation =
                        Duration.ofMillis(nowMs - effectiveStartTimeMs).toMinutes();

                String actualStatus = activities.fetchOrderStatus(orderId);
                String effectiveStatus = actualStatus;

                // Handle rider cancellation deterministically
                if ("RIDER_CANCELLED".equalsIgnoreCase(actualStatus)
                        && !"RIDER_CANCELLED".equalsIgnoreCase(prevActualStatus)) {

                    log.info("Order {} rider cancelled. Resetting tracking window.", orderId);

                    effectiveStartTimeMs = nowMs;
                    minutesSinceCreation = 0;
                    alertsSent = new boolean[alertTimings.length];

                    for (int i = 0; i < currentStatuses.length; i++) {
                        if ("SEARCHING_RIDER".equalsIgnoreCase(currentStatuses[i])) {
                            currentStartIndex = i;
                            break;
                        }
                    }
                    effectiveStatus = "SEARCHING_RIDER";
                }

                log.info(
                        "Order {} status after {} minutes: {} (effectiveStatus={}, index={})",
                        orderId,
                        minutesSinceCreation,
                        actualStatus,
                        effectiveStatus,
                        currentStartIndex);

                // Completion states
                if ("DELIVERED".equalsIgnoreCase(actualStatus) || "CANCELLED".equalsIgnoreCase(actualStatus)) {
                    log.info("Order {} completed with status {}", orderId, actualStatus);
                    return;
                }

                // Timeout
                if (minutesSinceCreation >= MAX_WAIT_MINUTES) {
                    log.warn("Order {} exceeded {} minutes. Completing workflow.", orderId, MAX_WAIT_MINUTES);
                    activities.sendOrderTrackAlert(orderId, minutesSinceCreation, nextExpectedFor(actualStatus));
                    return;
                }

                // Alerts
                for (int idx = currentStartIndex; idx < alertTimings.length; idx++) {
                    int shiftedIndex = idx - currentStartIndex;
                    if (shiftedIndex < 0 || shiftedIndex >= alertTimings.length) continue;

                    if (!alertsSent[idx] && minutesSinceCreation >= alertTimings[shiftedIndex]) {
                        if (currentStatuses[idx].equalsIgnoreCase(effectiveStatus)) {
                            activities.sendOrderTrackAlert(
                                    orderId, minutesSinceCreation, nextExpectedFor(effectiveStatus));
                            log.warn(
                                    "Alert fired: orderId={} status={} minutes={}",
                                    orderId,
                                    effectiveStatus,
                                    minutesSinceCreation);
                        }
                        alertsSent[idx] = true;
                    }
                }

                prevActualStatus = actualStatus;
            }

        } catch (Exception e) {
            log.error("Order track failed for orderId {}", orderId, e);
            throw Workflow.wrap(e);
        }
    }
}
