package com.etricky.cryptobot.core.exchanges.common;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExchangeLock {
	private HashMap<String, ReentrantLock> lockMap = new HashMap<>();

	public void getLock(String exchange) {
		try {
			if (lockMap.size() == 0 || !lockMap.containsKey(exchange)) {
				log.debug("adding new lock");
				lockMap.put(exchange, new ReentrantLock(true));
			}

			lockMap.get(exchange).lockInterruptibly();
			log.debug("got the lock");
		} catch (InterruptedException e) {
			log.debug("interrupted while getting the lock");

			releaseLock(exchange);
		}
	}

	public void releaseLock(String exchange) {
		if (lockMap.containsKey(exchange)) {
			if (lockMap.get(exchange).isHeldByCurrentThread()) {
				lockMap.get(exchange).unlock();
				log.debug("released the lock");
			}
		} else
			log.debug("no lock for exchange");
	}
}
