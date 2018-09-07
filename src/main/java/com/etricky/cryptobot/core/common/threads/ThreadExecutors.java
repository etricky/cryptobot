package com.etricky.cryptobot.core.common.threads;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.etricky.cryptobot.core.exchanges.common.exceptions.ExchangeException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ThreadExecutors {
	SimpleAsyncTaskExecutor executor;
	Map<String, ThreadPoolTaskExecutor> threadPoolExecutorMap = new HashMap<>();
	Map<String, BlockingQueue<Object>> blockingQueueMap = new HashMap<>();

	public ThreadExecutors() {

	}

	public void executeSingle(Runnable thread) {
		if (executor == null) {
			executor = new SimpleAsyncTaskExecutor();
			executor.setConcurrencyLimit(SimpleAsyncTaskExecutor.UNBOUNDED_CONCURRENCY);
		}
		executor.execute(thread);
	}

	public void initializeThreadPool(String poolName, int maxPoolSize, int queueCapacity) {
		log.debug("start. poolName: {} maxPoolSize: {} queueCapacity: {}", poolName, maxPoolSize, queueCapacity);

		if (!threadPoolExecutorMap.containsKey(poolName)) {
			ThreadPoolTaskExecutor threadPoolExecutor = new ThreadPoolTaskExecutor();
			threadPoolExecutor.setCorePoolSize(maxPoolSize);
			threadPoolExecutor.setMaxPoolSize(maxPoolSize);
			threadPoolExecutor.setQueueCapacity(queueCapacity);
			threadPoolExecutor.initialize();
			threadPoolExecutorMap.put(poolName, threadPoolExecutor);
		}

		log.debug("done");
	}

	public void initializeBlockingQueue(String poolName) {
		log.debug("start. poolName: {}", poolName);

		blockingQueueMap.put(poolName, new ArrayBlockingQueue<Object>(4));

		log.debug("done");
	}

	public boolean offerTask(String poolName, Object task) throws ExchangeException {
		log.debug("start. poolName: {}", poolName);

		if (blockingQueueMap.containsKey(poolName)) {
			return blockingQueueMap.get(poolName).offer(task);
		}

		log.error("no blocking queue: {}", poolName);
		throw new ExchangeException("no blocking queue: " + poolName);
	}

	public Object takeTask(String poolName) throws ExchangeException, InterruptedException {
		log.debug("start. poolName: {}", poolName);

		if (blockingQueueMap.containsKey(poolName)) {
			return blockingQueueMap.get(poolName).take();
		}

		log.error("no blocking queue: {}", poolName);
		throw new ExchangeException("no blocking queue: " + poolName);
	}

	public void executeTaskOnThreadPool(String poolName, Runnable thread) throws ExchangeException {
		if (threadPoolExecutorMap.containsKey(poolName)) {
			threadPoolExecutorMap.get(poolName).execute(thread);
		} else {
			log.error("no pool {} found", poolName);
			throw new ExchangeException("No thread pool found for " + poolName);
		}

	}

	public void destroyThreadPools() {
		log.debug("start");

		threadPoolExecutorMap.values().forEach(poolExecutor -> {
			poolExecutor.destroy();
		});
	}
}
