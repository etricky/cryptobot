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
	private ExchangeGeneric exchangeGeneric;
	@NonNull
	String threadName;
	Thread thread;

	public void setThread(Thread thread, UncaughtExceptionHandler excHandler) {
		this.thread=thread;
		this.thread.setUncaughtExceptionHandler(excHandler);
		this.thread.setName(threadName);
	}
	
	public void interrupt() {
		log.debug("start");
		thread.interrupt();
		log.debug("done");
	}

}
