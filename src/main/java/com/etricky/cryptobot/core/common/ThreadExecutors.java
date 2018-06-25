package com.etricky.cryptobot.core.common;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class ThreadExecutors {
	SimpleAsyncTaskExecutor executor;

	public ThreadExecutors() {
		executor = new SimpleAsyncTaskExecutor();
		executor.setConcurrencyLimit(SimpleAsyncTaskExecutor.UNBOUNDED_CONCURRENCY);
	}

	public void executeSingle(Runnable thread) {
		executor.execute(thread);
	}

}
