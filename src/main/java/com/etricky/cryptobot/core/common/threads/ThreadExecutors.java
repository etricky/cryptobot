package com.etricky.cryptobot.core.common.threads;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ThreadExecutors {
	SimpleAsyncTaskExecutor executor;
	ThreadPoolTaskExecutor threadPoolExecutor;

	public ThreadExecutors() {

	}

	public void executeSingle(Runnable thread) {
		if (executor == null) {
			executor = new SimpleAsyncTaskExecutor();
			executor.setConcurrencyLimit(SimpleAsyncTaskExecutor.UNBOUNDED_CONCURRENCY);
		}
		executor.execute(thread);
	}

	public void initializeThreadPool() {
		log.debug("start");

		if (threadPoolExecutor == null) {
			threadPoolExecutor.setCorePoolSize(4);
			threadPoolExecutor.setMaxPoolSize(4);
			threadPoolExecutor.initialize();
		}

		log.debug("done");
	}

	public void executeTaskOnThreadPool(Runnable thread) {
		threadPoolExecutor.execute(thread);
	}

	public void destroyThreadPool() {
		threadPoolExecutor.shutdown();
	}
}
