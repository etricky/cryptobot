package com.etricky.cryptobot.core.exchanges.common;

import java.lang.Thread.UncaughtExceptionHandler;

import com.etricky.cryptobot.core.interfaces.shell.ShellCommands;
import com.etricky.cryptobot.repositories.TradesEntityRepository;

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
	protected ShellCommands shellCommands;
	@Getter
	protected TradesEntityRepository tradesEntityRepository;

	public ExchangeGeneric(ExchangeThreads exchangeThreads, ShellCommands shellCommands,
			TradesEntityRepository tradesEntityRepository) {
		this.exchangeThreads = exchangeThreads;
		this.shellCommands = shellCommands;
		this.tradesEntityRepository = tradesEntityRepository;
	}

	public void setThreadInfo(ThreadInfo threadInfo) {
		log.debug("start. threadInfo: {}", threadInfo);

		this.threadInfo = threadInfo;

		log.debug("done");
	}

	protected void stopTrade() {
		log.debug("start");

		if (subscription != null && !subscription.isDisposed()) {
			subscription.dispose();
			log.debug("subscription disposed");
		}

		if (exchange != null && exchange.isAlive()) {
			log.debug("disconnect");
			// Disconnect from exchange (non-blocking)
			exchange.disconnect().subscribe(() -> log.debug("Disconnected from exchange: {} currency: {}",
					threadInfo.getExchangeEnum().getName(), threadInfo.getCurrencyEnum().getShortName()));
		} else {
			log.debug("exchange is not alive!");
		}

		exchangeThreads.removeThread(threadInfo);

		shellCommands.sendMessage("Stopped thread: " + threadInfo.getThreadName(), true);

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

		log.debug("done");
	}

}
