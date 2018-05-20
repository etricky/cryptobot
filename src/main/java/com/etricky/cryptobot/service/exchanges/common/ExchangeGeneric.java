package com.etricky.cryptobot.service.exchanges.common;

import org.springframework.beans.factory.annotation.Autowired;

import com.etricky.cryptobot.service.interfaces.shell.ShellCommands;

import info.bitrich.xchangestream.core.StreamingExchange;
import io.reactivex.disposables.Disposable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public abstract class ExchangeGeneric extends Thread {

	protected ThreadInfo threadInfo;
	protected Disposable subscription;
	protected StreamingExchange exchange;
	@Autowired
	protected ExchangeThreads exchangeThreads;
	@Autowired
	protected ShellCommands shellCommands;

	public void startThread(ThreadInfo threadInfo) {
		log.debug("start. threadInfo: {}", threadInfo);

		this.threadInfo = threadInfo;
		this.setName(threadInfo.getThreadKey());
		this.start();

		log.debug("done");
	}

	protected void stopTrade() {
		log.debug("start");

		subscription.dispose();

		log.debug("disconnect");
		// Disconnect from exchange (non-blocking)
		exchange.disconnect().subscribe(() -> log.info("Disconnected from the exchange: {} currency: {}",
				threadInfo.getExchangeEnum().getName(), threadInfo.getCurrencyEnum().getShortName()));

		exchangeThreads.removeThread(threadInfo);

		shellCommands.sendMessage("Stopping thread: " + threadInfo.getThreadKey(), true);

		log.debug("done");
	}
}
