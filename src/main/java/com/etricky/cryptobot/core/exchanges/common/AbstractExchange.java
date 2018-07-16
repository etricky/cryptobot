package com.etricky.cryptobot.core.exchanges.common;

import java.lang.Thread.UncaughtExceptionHandler;

import com.etricky.cryptobot.core.interfaces.Commands;
import com.etricky.cryptobot.core.interfaces.jsonFiles.JsonFiles;
import com.etricky.cryptobot.core.strategies.common.ExchangeStrategy;
import com.etricky.cryptobot.model.TradesEntity;

import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.disposables.Disposable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractExchange implements Runnable, UncaughtExceptionHandler {

	public static final int TRADE_ALL = 0;
	public static final int TRADE_HISTORY = 1;
	public static final int TRADE_LIVE = 2;

	@Getter
	protected ThreadInfo threadInfo;
	protected Disposable subscription;
	protected StreamingExchange exchange;
	protected ExchangeThreads exchangeThreads;
	protected Commands commands;
	protected JsonFiles jsonFiles;
	protected ExchangeStrategy exchangeStrategy;
	@Setter
	protected int tradeType;

	public AbstractExchange(ExchangeThreads exchangeThreads, Commands commands, JsonFiles jsonFiles, ExchangeStrategy exchangeStrategy) {
		this.exchangeThreads = exchangeThreads;
		this.commands = commands;
		this.jsonFiles = jsonFiles;
		this.exchangeStrategy = exchangeStrategy;
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
				exchange.disconnect().subscribe(() -> log.debug("Disconnected from exchange: {} currency: {}", threadInfo.getExchangeEnum().getName(),
						threadInfo.getCurrencyEnum().getShortName()));
			} else {
				log.debug("exchange is not alive!");
			}
		} catch (Exception e) {
			log.error("Exception: {}", e);
		}

		exchangeThreads.removeThread(threadInfo);

		commands.sendMessage("Stopped trade for exchange: " + threadInfo.getExchangeEnum().getCrytobotBean() + " currency: "
				+ threadInfo.getCurrencyEnum().getShortName(), true);

		log.debug("done");
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error("start. exception on thread:{}", t.getName());
		log.error("exception: {}", e);

		commands.sendMessage("Exception occurred on " + t.getName() + ". Stopping thread", true);
		// sends the interrupt to itself
		if (t.isAlive() || !t.isInterrupted()) {
			log.debug("sending interrupt");
			t.interrupt();
		}

		// in case the interrupt hasn't stopped the thread
		stopTrade();

		log.debug("done");
	}

	public void processStrategyForLiveTrade(TradesEntity tradesEntity) throws ExchangeException {
		log.trace("start");

		exchangeStrategy.processStrategyForLiveTrade(tradesEntity);

		log.trace("done");
	}

	public void addHistoryTradeToTimeSeries(TradesEntity tradesEntity) throws ExchangeException {
		log.trace("start");

		exchangeStrategy.addHistoryTradeToTimeSeries(tradesEntity);

		log.trace("done");
	}

}
