package com.hyp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;

@Configuration
@ConditionalOnProperty(name = "spring.scheduler.enabled")
@EnableScheduling
public class TaskSchedulerConfig implements SchedulingConfigurer {

    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10); // Set the number of threads in the pool
        taskScheduler.setThreadNamePrefix("ThreadScheduler-");
        taskScheduler.initialize(); // Initialize the task scheduler
        return taskScheduler;
    }

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setTaskScheduler(taskScheduler());
	}
}
