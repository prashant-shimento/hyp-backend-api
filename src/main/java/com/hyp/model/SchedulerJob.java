package com.hyp.model;

import lombok.Data;

@Data
public class SchedulerJob {

	private String jobId;
	private String jobName;
	private String executionTime;
}
