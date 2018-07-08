package com.etricky.cryptobot.core.exchanges.common;

import java.lang.Thread.UncaughtExceptionHandler;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@ToString
@RequiredArgsConstructor
@Slf4j
public class ThreadInfo {

	@NonNull
	private ExchangeEnum exchangeEnum;
	@NonNull
	private CurrencyEnum currencyEnum;
	@NonNull
	private AbstractExchange exchangeGeneric;
	@NonNull
	private String threadName;
	private Thread thread;

	public void setThread(Thread thread, UncaughtExceptionHandler excHandler) {
		this.thread = thread;
		this.thread.setUncaughtExceptionHandler(excHandler);
		this.thread.setName(threadName);
		log.debug("thread: {} id: {}", threadName, thread.getId());
	}

	public void interrupt() {
		log.debug("start. thread: {} id: {} state: {} current thread: {} id: {}", threadName, thread.getId(),
				thread.getState(), Thread.currentThread().getName(), Thread.currentThread().getId());

		thread.interrupt();

		if (thread.isInterrupted())
			log.debug("thread is interrupted");
		else {
			log.debug("thread still lives!!!");
		}
		log.debug("done");
	}

}
