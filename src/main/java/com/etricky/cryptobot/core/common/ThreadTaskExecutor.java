package com.etricky.cryptobot.core.common;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class ThreadTaskExecutor {
	SimpleAsyncTaskExecutor executor;

	public ThreadTaskExecutor() {
		executor = new SimpleAsyncTaskExecutor();
		executor.setConcurrencyLimit(SimpleAsyncTaskExecutor.UNBOUNDED_CONCURRENCY);
	}

	public void execute(Runnable thread) {
		executor.execute(thread);
	}

}
