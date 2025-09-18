package com.hyp.temporal.workflow;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.hyp.entity.Order;
import com.hyp.temporal.activities.OrderTrackActivities;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderTrackWorkFlowImpl implements OrderTrackWorkFlow {

	private final OrderTrackActivities activities = Workflow.newActivityStub(OrderTrackActivities.class,
			ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofMinutes(5)).build());

	private String nextExpectedFor(String currentStatus) {
		if (currentStatus == null)
			return "progress to next stage";
		switch (currentStatus.trim().toUpperCase()) {
		case "PAID":
			return "ACCEPTED";
		case "ACCEPTED":
			return "SEARCHING_RIDER";
		case "SEARCHING_RIDER":
			return "OUT_FOR_PICKUP";
		case "OUT_FOR_PICKUP":
			return "OUT_FOR_DELIVERY";
		case "OUT_FOR_DELIVERY":
			return "DELIVERED";
		case "DELIVERED":
			return "DELIVERED";
		default:
			return "progress to the next expected status";
		}
	}

	@Override
	public void handleOrderTrack(String orderId) {
		log.info("Workflow started: handleOrderTrackWithTimeout for orderId={}", orderId);

		try {
			Order order = activities.fetchOrder(orderId);
			LocalDateTime orderCreatedAt = order.getCreatedAt();
			log.info("Order {} created at: {}", orderId, orderCreatedAt);

			final int MAX_WAIT_MINUTES = 90;

			final int[] alertTimings = { 5, 7, 17, 30, 45, 50 };
			final String[] expectedStatuses = { "ACCEPTED", "SEARCHING_RIDER", "OUT_FOR_PICKUP", "OUT_FOR_DELIVERY",
					"DELIVERED", "DELIVERED" };
			final String[] currentStatuses = { "PAID", "ACCEPTED", "SEARCHING_RIDER", "OUT_FOR_PICKUP",
					"OUT_FOR_DELIVERY", "OUT_FOR_DELIVERY" };

			boolean[] alertsSent = new boolean[alertTimings.length];

			// default 0 (start from PAID -> ACCEPTED -> ...)
			int currentStartIndex = 0;

			// initialize previous status so we can detect transitions
			String prevActualStatus = activities.fetchOrderStatus(orderId);

			// CONTINUOUS MONITORING WITH TIMEOUT
			while (true) {
				Workflow.sleep(Duration.ofMinutes(1));

				String actualStatus = activities.fetchOrderStatus(orderId);
				long minutesSinceCreation = ChronoUnit.MINUTES.between(orderCreatedAt, LocalDateTime.now());

				// If rider got cancelled, restart the cycle from SEARCHING_RIDER (as requested)
				String effectiveStatus = actualStatus;
				if ("RIDER_CANCELLED".equalsIgnoreCase(actualStatus)) {
					// Only reset once when the transition into RIDER_CANCELLED is observed
					if (!"RIDER_CANCELLED".equalsIgnoreCase(prevActualStatus)) {
						log.info(
								"Order {} transitioned to RIDER_CANCELLED. Resetting alert timings and treating as SEARCHING_RIDER.",
								orderId);

						// restart timing from now
						orderCreatedAt = LocalDateTime.now();
						minutesSinceCreation = 0;

						// Reset alertsSent so new alerts can fire for the SEARCHING_RIDER cycle
						alertsSent = new boolean[alertTimings.length];

						// Start alert-checking from the SEARCHING_RIDER index
						for (int idx = 0; idx < currentStatuses.length; idx++) {
							if ("SEARCHING_RIDER".equalsIgnoreCase(currentStatuses[idx])) {
								currentStartIndex = idx;
								break;
							}
						}

						effectiveStatus = "SEARCHING_RIDER";
					} else {
						// repeated RIDER_CANCELLED checks — treat as SEARCHING_RIDER for alerts,
						// but do not re-reset again (already reset once)
						effectiveStatus = "SEARCHING_RIDER";
					}
				}

				log.info("Order {} status after {} minutes: {} (effectiveStatus={}, currentStartIndex={})", orderId,
						minutesSinceCreation, actualStatus, effectiveStatus, currentStartIndex);

				if ("DELIVERED".equalsIgnoreCase(actualStatus) || "CANCELLED".equalsIgnoreCase(actualStatus)) {
					log.info("Order {} {} after {} minutes since creation. WORKFLOW COMPLETING SUCCESSFULLY.", orderId,
							actualStatus.toUpperCase(), minutesSinceCreation);
					return;
				}

				// TIMEOUT CHECK - Complete workflow after maximum wait time
				if (minutesSinceCreation >= MAX_WAIT_MINUTES) {
					log.warn(
							"Order {} exceeded maximum wait time of {} minutes. Final status: {}. WORKFLOW COMPLETING WITH TIMEOUT.",
							orderId, MAX_WAIT_MINUTES, actualStatus);
					String nextExpected = nextExpectedFor(actualStatus);
					activities.sendOrderTrackAlert(orderId, minutesSinceCreation, nextExpected);
					return;
				}

				// SEND PRE-ALERTS - start from currentStartIndex; shift alert timings so the
				// first
				// timing after reset corresponds to alertTimings[0]).
				for (int idx = currentStartIndex; idx < alertTimings.length; idx++) {
					int shiftedIndex = idx - currentStartIndex;
					if (shiftedIndex < 0 || shiftedIndex >= alertTimings.length)
						continue;

					if (minutesSinceCreation >= alertTimings[shiftedIndex] && !alertsSent[idx]) {

						// Use effectiveStatus to check if order is stuck in current status when it
						// should have progressed
						if (currentStatuses[idx].equalsIgnoreCase(effectiveStatus)) {
							String alertMessage = "";

							if (idx < 5) {
								alertMessage = String.format(
										"PRE-ALERT: Order %s stuck in %s status for %d minutes. Expected to be %s by now.",
										orderId, effectiveStatus, minutesSinceCreation, expectedStatuses[idx]);
							} else {
								alertMessage = String.format(
										"CRITICAL ALERT: Order %s still OUT_FOR_DELIVERY after %d minutes. Customer likely waiting - immediate action needed!",
										orderId, minutesSinceCreation);
							}

							log.warn(alertMessage);
							String nextExpected = nextExpectedFor(effectiveStatus);
							activities.sendOrderTrackAlert(orderId, minutesSinceCreation, nextExpected);
						} else {
							log.info("Order {} has progressed past {} status at {} minutes - no alert needed", orderId,
									currentStatuses[idx], minutesSinceCreation);
						}

						alertsSent[idx] = true;
					}
				}

				prevActualStatus = actualStatus;
			}

		} catch (Exception e) {
			log.error("handleOrderTrackWithTimeout failed for orderId={}", orderId, e);
			throw new RuntimeException("Order track failed for orderId=" + orderId, e);
		}
	}

}
