package com.hyp.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import com.hyp.entity.Delivery;
import com.hyp.model.SchedulerJob;
import com.hyp.translation.DeliveryRequestTranslation;

public class SchedulerService {

	private Map<String, SchedulerJob> schedulerMap = new HashMap<>();

	@Autowired
	TaskScheduler taskScheduler;

	@Autowired
	DeliveryService deliveryService;

	public void schedulerJob(SchedulerJob job, Trigger trigger, Delivery delivery) {
		taskScheduler.schedule(() -> {
			System.out.println("Delivery Order scheduled via Scheduler for " + delivery.getDeliveryOrderId()
					+ delivery.getDeliveryScheduledAt());
			boolean completed = deliveryService
					.initiateOrderFulfill(DeliveryRequestTranslation.getOrderFulfillRequest(delivery));
			if (completed) {
				System.out.println("Scheduled task completed successfully.");
			}
		}, trigger);

		schedulerMap.put(job.getJobName() + "-" + System.currentTimeMillis(), job);
	}
}
