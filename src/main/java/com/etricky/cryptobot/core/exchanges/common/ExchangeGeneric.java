package com.etricky.cryptobot.core.exchanges.common;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.common.StrategyGeneric;
import com.etricky.cryptobot.model.TradesEntity;

import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.disposables.Disposable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ExchangeGeneric implements Runnable, UncaughtExceptionHandler {

	@Getter
	protected ThreadInfo threadInfo;
	protected Disposable subscription;
	protected StreamingExchange exchange;
	protected ExchangeThreads exchangeThreads;
	protected Commands commands;
	protected JsonFiles jsonFiles;
	@Getter
	protected List<StrategyGeneric> strategies;

	public ExchangeGeneric(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles) {
		this.exchangeThreads = exchangeThreads;
		this.commands = commands;
		this.jsonFiles = jsonFiles;
	}

	public void setThreadInfo(ThreadInfo threadInfo) {
		log.debug("start. threadInfo: {}", threadInfo);

		this.threadInfo = threadInfo;
		log.debug("thread: {}", Thread.currentThread().getId());

		log.debug("done");
	}

	protected void setThreadInfoData() {
		log.debug("start");

		log.debug("thread: {}", Thread.currentThread().getId());
		threadInfo.setThread(Thread.currentThread(), this);

		log.debug("done");
	}

	protected void stopTrade() {
		log.debug("start");

		try {
			if (subscription != null && !subscription.isDisposed()) {
				subscription.dispose();
				log.debug("subscription disposed");
			}

			if (exchange != null && exchange.isAlive()) {
				log.debug("disconnect from exchange");
				// Disconnect from exchange (non-blocking)
				exchange.disconnect().subscribe(() -> log.debug("Disconnected from exchange: {} currency: {}",
						threadInfo.getExchangeEnum().getName(), threadInfo.getCurrencyEnum().getShortName()));
			} else {
				log.debug("exchange is not alive!");
			}
		} catch (Exception e) {
			log.error("Exception: {}", e);
		}

		exchangeThreads.removeThread(threadInfo);

		commands.sendMessage("Stopped thread: " + threadInfo.getThreadName(), true);

		log.debug("done");
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("start. exception on thread:{}", t.getName());
		log.error("exception: {}", e);

		// sends the interrupt to itself
		if (t.isAlive() || !t.isInterrupted()) {
			log.debug("sending interrupt");
			t.interrupt();
		}

		// in case the interrupt hasn't stopped the thread
		stopTrade();

		log.debug("done");
	}

	public void processStrategyTrade(TradesEntity tradesEntity) {
		log.debug("start");

		strategies.forEach(s -> s.processLiveTrade(tradesEntity));

		log.debug("done");
	}

	public void addTradeToTimeSeries(TradesEntity tradesEntity) {
		log.debug("start");

		strategies.forEach(s -> s.addTradeToTimeSeries(tradesEntity));

		log.debug("done");
	}

}
